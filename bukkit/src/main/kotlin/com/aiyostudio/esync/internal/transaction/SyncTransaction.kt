package com.aiyostudio.esync.internal.transaction

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.internal.enums.TransactionState
import com.aiyostudio.esync.internal.handler.CacheHandler
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

@Deprecated("Temporary transaction logic.")
class SyncTransaction(
    private val uuid: UUID,
    private val modules: List<String>,
    private val success: (uuid: UUID) -> Unit = {},
    private val failed: (uuid: UUID) -> Unit = {}
) {
    private val completeList = mutableSetOf<String>()
    private var cancelled = false

    fun start() {
        val repository = RepositoryHandler.repository ?: run {
            this.failed.invoke(uuid)
            return
        }
        val list = modules.map { k ->
            CompletableFuture.supplyAsync async@{
                val module = ModuleHandler.findByKey(k) ?: return@async TransactionState.FAILED
                if (!repository.isExists(uuid, module.uniqueKey)) {
                    val bytea = module.toByteArray(uuid)
                    return@async if (module.firstLoad(uuid, bytea)) {
                        this.completeList.add(module.uniqueKey)
                        TransactionState.COMPLETE
                    } else {
                        TransactionState.FAILED
                    }
                }
                module.preLoad(uuid)
                if (repository.queryState(uuid, module.uniqueKey) != SyncState.COMPLETE) {
                    return@async TransactionState.FAILED
                }
                val bytea = repository.queryData(uuid, module.uniqueKey)
                if (!module.attemptLoad(uuid, bytea)) {
                    return@async TransactionState.FAILED
                }
                if (!repository.updateState(uuid, module.uniqueKey, SyncState.WAITING)) {
                    return@async TransactionState.FAILED
                }
                this.completeList.add(module.uniqueKey)
                return@async TransactionState.COMPLETE
            }.exceptionally {
                EfficientSyncBukkit.instance.logger.log(Level.WARNING, it) { "Failed to sync $k data" }
                TransactionState.FAILED
            }
        }.toList()
        CompletableFuture.allOf(*list.toTypedArray())
            .thenApply {
                list.stream().allMatch { v ->
                    return@allMatch v.isDone && v.get() == TransactionState.COMPLETE
                }
            }
            .thenAccept { allCompleted ->
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline && CacheHandler.playerCaches.containsKey(uuid) && allCompleted && !cancelled) {
                    this.apply(player)
                    success.invoke(uuid)
                } else {
                    this.completeList.forEach { v -> repository.updateState(uuid, v, SyncState.COMPLETE) }
                    failed.invoke(uuid)
                }
            }
    }

    private fun apply(player: Player) {
        val repository = RepositoryHandler.repository ?: return
        this.modules.forEach {
            repository.updateState(uuid, it, SyncState.LOCKED)
            // apply data
            val module = ModuleHandler.findByKey(it)!!
            module.apply(player.uniqueId)
            CacheHandler.playerCaches[uuid]!!.load(it)
        }
    }
}