package com.aiyostudio.esync.internal.handler

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.internal.cache.PlayerCache
import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.logging.Level

object CacheHandler {
    val playerCaches = mutableMapOf<UUID, PlayerCache>()
    val dependModules = mutableSetOf<String>()
    private val logger = EfficientSyncBukkit.instance.logger

    private fun debug(message: String) {
        if (SyncConfig.debug) {
            logger.info("[eSync-Debug] $message")
        }
    }

    fun removeAndSaved(player: Player) {
        val uuid = player.uniqueId
        val cache = playerCaches.remove(uuid)
        if (cache == null) {
            debug("[Save] No cache found for ${player.name} ($uuid)")
            return
        }
        
        val loadedModules = cache.getLoadedModules()
        debug("[Save] Saving ${loadedModules.size} modules for ${player.name}: ${loadedModules.joinToString(", ")}")
        
        loadedModules.forEach { moduleKey ->
            val repository = RepositoryHandler.repository
            if (repository == null) {
                logger.warning("[eSync] Repository not available when saving $moduleKey for ${player.name}")
                return@forEach
            }
            
            val module = ModuleHandler.findByKey(moduleKey)
            if (module == null) {
                logger.warning("[eSync] Module '$moduleKey' not found when saving for ${player.name}")
                return@forEach
            }
            
            val bytea = module.toByteArray(uuid)
            if (bytea == null) {
                logger.warning("[eSync] Failed to serialize module '$moduleKey' for ${player.name}")
                return@forEach
            }
            
            debug("[Save] [$moduleKey] Serialized data size: ${bytea.size} bytes")
            module.unloadCache(uuid)
            
            Bukkit.getScheduler().runTaskAsynchronously(EfficientSyncBukkit.instance) {
                try {
                    debug("[Save] [$moduleKey] Inserting data with state LOCKED...")
                    val insertResult = repository.insert(uuid, module.uniqueKey, bytea, SyncState.LOCKED)
                    debug("[Save] [$moduleKey] Insert result: $insertResult")
                    
                    if (insertResult) {
                        debug("[Save] [$moduleKey] Updating state to COMPLETE...")
                        val updateResult = repository.updateState(uuid, module.uniqueKey, SyncState.COMPLETE)
                        debug("[Save] [$moduleKey] Update state result: $updateResult")
                        
                        if (!updateResult) {
                            logger.warning("[eSync] Failed to update state to COMPLETE for module '$moduleKey', player: ${player.name}")
                        }
                    } else {
                        logger.warning("[eSync] Failed to insert data for module '$moduleKey', player: ${player.name}")
                    }
                } catch (e: Exception) {
                    logger.log(Level.WARNING, e) { "[eSync] Exception while saving module '$moduleKey' for ${player.name}" }
                }
            }
        }
    }
}