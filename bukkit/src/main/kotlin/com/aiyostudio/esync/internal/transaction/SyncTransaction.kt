package com.aiyostudio.esync.internal.transaction

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.enums.TransactionState
import com.aiyostudio.esync.internal.handler.CacheHandler
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.util.PlayerUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import kotlin.Exception

class SyncTransaction(
    private val uuid: UUID,
    private val modules: List<String>,
    private val success: (uuid: UUID) -> Unit = {},
    private val failed: (uuid: UUID) -> Unit = {}
) {
    private val completeList = mutableSetOf<String>()
    private var cancelled = false

    fun start() {
        val list = modules.map { k ->
            CompletableFuture.supplyAsync {
                if (PlayerUtil.isOnline(uuid)) {
                    val task = SyncTask(uuid, k).execute()
                    val state = task.getState()
                    if (state == TransactionState.COMPLETE) {
                        completeList.add(k)
                    }
                    state
                } else {
                    TransactionState.FAILED
                }
            }.exceptionally {
                EfficientSyncBukkit.instance.logger.log(Level.WARNING, it) { "Failed to sync $k data" }
                TransactionState.FAILED
            }
        }.toList()
        CompletableFuture.allOf(*list.toTypedArray())
            .thenApply { list.stream().allMatch { v -> v.isDone && v.get() == TransactionState.COMPLETE } }
            .thenAccept { allCompleted ->
                Bukkit.getPlayer(uuid)?.takeIf { it.isOnline }?.let {
                    val result = CacheHandler.playerCaches.containsKey(uuid) && allCompleted && !cancelled
                    if (result) apply(it) else failed(uuid)
                } ?: failed()
            }
    }

    private fun failed() {
        val repository = RepositoryHandler.repository ?: return
        this.completeList.forEach { v -> repository.updateState(uuid, v, SyncState.COMPLETE) }
        failed(uuid)
    }

    private fun apply(player: Player) {
        Bukkit.getScheduler().runTask(EfficientSyncBukkit.instance) {
            try {
                val repository = RepositoryHandler.repository ?: return@runTask
                val result = this.modules.all {
                    repository.updateState(uuid, it, SyncState.LOCKED)
                    val module = ModuleHandler.findByKey(it)!!
                    if (module.apply(player.uniqueId)) {
                        CacheHandler.playerCaches[player.uniqueId]!!.load(module.uniqueKey)
                        return@all true
                    }
                    false
                }
                if (result) success(uuid) else failed()
            } catch (e: Exception) {
                EfficientSyncBukkit.instance.logger.log(Level.WARNING, e) { "Failed to apply ${player.name} data." }
            }
        }
    }
}

private class SyncTask(
    private val uuid: UUID,
    private val moduleId: String
) {
    private val module = ModuleHandler.findByKey(moduleId)
    private val repository = RepositoryHandler.repository
    private var stage = SyncTaskStage.WAITING
    private var state: TransactionState = TransactionState.FAILED

    private fun preload(): Boolean {
        if (repository!!.isExists(uuid, module!!.uniqueKey)) {
            module.preLoad(uuid)
            return true
        }
        val bytea = module.toByteArray(uuid)
        if (module.firstLoad(uuid, bytea)) {
            state = TransactionState.COMPLETE
            return true
        }
        return false
    }

    private fun attemptLoad(): Boolean {
        if (state == TransactionState.COMPLETE) return true
        val autoUnlock = SyncConfig.autoUnlock?.getBoolean("enable", true) ?: true
        val tick = 1.coerceAtLeast(if (autoUnlock) SyncConfig.autoUnlock?.getInt("delay", 20) ?: 20 else 1)
        var syncState: SyncState? = null
        for (i in 0 until tick) {
            if (!PlayerUtil.isOnline(uuid)) {
                break
            }
            syncState = repository!!.queryState(uuid, module!!.uniqueKey)
            if (syncState == SyncState.COMPLETE || !autoUnlock) {
                break
            }
            if (i + 1 == tick && repository.updateState(uuid, module.uniqueKey, SyncState.COMPLETE)) {
                syncState = SyncState.COMPLETE
                break
            }
            Thread.sleep(1000L)
        }
        if (syncState == SyncState.COMPLETE) {
            val bytea = repository!!.queryData(uuid, module!!.uniqueKey)
            if (!module.attemptLoad(uuid, bytea)) return false
            if (!repository.updateState(uuid, module.uniqueKey, SyncState.WAITING)) return false
            state = TransactionState.COMPLETE
            return true
        }
        return false
    }

    fun execute(): SyncTask {
        if (module == null || repository == null) {
            this.state = TransactionState.FAILED
            return this
        }
        if (this.stage == SyncTaskStage.WAITING && this.preload()) {
            stage = SyncTaskStage.PRELOAD
        }
        if (this.stage == SyncTaskStage.PRELOAD) {
            this.attemptLoad()
        }
        return this
    }

    fun getState(): TransactionState {
        return this.state
    }
}

private enum class SyncTaskStage {
    WAITING,
    PRELOAD
}