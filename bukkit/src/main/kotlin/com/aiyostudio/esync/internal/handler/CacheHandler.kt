package com.aiyostudio.esync.internal.handler

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.internal.cache.PlayerCache
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

object CacheHandler {
    val playerCaches = mutableMapOf<UUID, PlayerCache>()
    val dependModules = mutableListOf<String>()

    fun removeAndSaved(player: Player) {
        CacheHandler.playerCaches.remove(player.uniqueId)?.getLoadedModules()?.forEach {
            val uuid = player.uniqueId
            val repository = RepositoryHandler.repository ?: return@forEach
            val module = ModuleHandler.findByKey(it) ?: return@forEach
            val bytea = module.toByteArray(uuid) ?: return@forEach
            module.unloadCache(uuid)
            Bukkit.getScheduler().runTaskAsynchronously(EfficientSyncBukkit.instance) {
                if (repository.insert(uuid, module.uniqueKey, bytea, SyncState.LOCKED)) {
                    repository.updateState(uuid, module.uniqueKey, SyncState.COMPLETE)
                }
            }
        }
    }
}