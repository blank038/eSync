package com.aiyostudio.esync.internal.transaction

import com.aiyostudio.esync.common.EfficientSync
import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.internal.api.event.PlayerModuleEvent
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
                    } else {
                        EfficientSyncBukkit.instance.logger.warning("Failed to sync module '$k' for player $uuid")
                    }
                    state
                } else {
                    EfficientSyncBukkit.instance.logger.warning("Player $uuid is offline, skipping sync module '$k'")
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
                    val hasCache = CacheHandler.playerCaches.containsKey(uuid)
                    val result = hasCache && allCompleted && !cancelled
                    if (result) {
                        syncApply(it)
                    } else {
                        if (!hasCache) {
                            EfficientSyncBukkit.instance.logger.warning("Player cache not found for $uuid")
                        }
                        if (cancelled) {
                            EfficientSyncBukkit.instance.logger.warning("Sync transaction cancelled for $uuid")
                        }
                        failed(uuid)
                    }
                } ?: run {
                    EfficientSyncBukkit.instance.logger.warning("Player $uuid is offline when applying data")
                    failed()
                }
            }
    }

    private fun failed() {
        val repository = RepositoryHandler.repository ?: return
        this.completeList.forEach { v -> repository.updateState(uuid, v, SyncState.COMPLETE) }
        failed(uuid)
    }

    private fun syncApply(player: Player) {
        Bukkit.getScheduler().runTask(EfficientSyncBukkit.instance) { apply(player) }
    }

    private fun apply(player: Player) {
        val playerCache = CacheHandler.playerCaches[player.uniqueId] ?: run {
            EfficientSyncBukkit.instance.logger.warning("Player cache not found when applying data for ${player.name}")
            failed(uuid)
            return
        }

        try {
            // 批量处理所有模块，减少线程切换开销
            val successfulModules = mutableListOf<String>()

            for (moduleId in modules) {
                try {
                    // 异步更新玩家模块状态
                    Bukkit.getScheduler().runTaskAsynchronously(EfficientSyncBukkit.instance) {
                        EfficientSync.api.updateState(uuid, moduleId, SyncState.LOCKED)
                    }
                    // 获取模块数据并应用模块数据
                    val module = ModuleHandler.findByKey(moduleId)
                    if (module != null && module.apply(player.uniqueId)) {
                        // 玩家缓存加载
                        playerCache.load(module.uniqueKey)
                        successfulModules.add(module.uniqueKey)
                    } else {
                        EfficientSyncBukkit.instance.logger.warning(
                            "Failed to apply module $moduleId for player ${player.name}"
                        )
                        failed(uuid)
                        return
                    }
                } catch (e: Exception) {
                    EfficientSyncBukkit.instance.logger.log(
                        Level.WARNING,
                        e
                    ) { "Failed to process module $moduleId for ${player.name}" }
                    failed(uuid)
                    return
                }
            }

            // 批量执行所有模块加载事件
            successfulModules.forEach { uniqueKey ->
                try {
                    val event = PlayerModuleEvent.Loaded(player, uniqueKey)
                    Bukkit.getPluginManager().callEvent(event)
                } catch (e: Exception) {
                    EfficientSyncBukkit.instance.logger.log(
                        Level.WARNING,
                        e
                    ) { "Failed to execute loaded event for module $uniqueKey" }
                    // 事件失败不影响其他事件处理
                }
            }

            // 如果需要，添加依赖加载事件
            try {
                if (playerCache.checkDepends()) {
                    val dependEvent = PlayerModuleEvent.DependLoaded(player)
                    Bukkit.getPluginManager().callEvent(dependEvent)
                }
            } catch (e: Exception) {
                EfficientSyncBukkit.instance.logger.log(
                    Level.WARNING,
                    e
                ) { "Failed to execute depend loaded event for ${player.name}" }
                // 事件失败不影响整体处理
            }

            // 最终执行成功回调
            success(uuid)
        } catch (e: Exception) {
            EfficientSyncBukkit.instance.logger.log(
                Level.WARNING,
                e
            ) { "Unexpected error while applying data for ${player.name}" }
            failed(uuid)
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