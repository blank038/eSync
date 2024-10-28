package com.aiyostudio.esync.internal.transaction

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.internal.enums.TransactionState
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture

class SyncTransaction(
    private val uuid: UUID,
    private val modules: List<String>,
    private val success: (uuid: UUID) -> Unit,
    private val failed: (uuid: UUID) -> Unit
) {
    private var cancelled = false

    fun start() {
        val list = modules.map { k ->
            CompletableFuture.supplyAsync {
                val module = ModuleHandler.findByKey(k) ?: return@supplyAsync TransactionState.FAILED
                val repository = RepositoryHandler.repository ?: return@supplyAsync TransactionState.FAILED
                val bytea = repository.queryData(uuid, module.uniqueKey)
                if (module.attemptLoad(uuid, bytea)) {
                    return@supplyAsync SyncState.COMPLETE
                }
                return@supplyAsync TransactionState.FAILED
            }.exceptionally { TransactionState.FAILED }
        }.toList()
        CompletableFuture.allOf(*list.toTypedArray())
            .thenApply { list.stream().allMatch { v -> v.isDone && v.get() == TransactionState.COMPLETE } }
            .thenAccept { allCompleted ->
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline && allCompleted && !cancelled) {
                    this.apply(player)
                    success.invoke(uuid)
                } else {
                    failed.invoke(uuid)
                }
            }
    }

    private fun apply(player: Player) {
        this.modules.forEach {
            val module = ModuleHandler.findByKey(it)
            module?.find(player.uniqueId)
        }
    }
}