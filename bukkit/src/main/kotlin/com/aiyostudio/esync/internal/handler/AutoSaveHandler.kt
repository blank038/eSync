package com.aiyostudio.esync.internal.handler

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.util.LoggerUtil
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/**
 * 自动保存处理器
 * 负责定期保存玩家数据，但不卸载缓存
 */
object AutoSaveHandler {
    private var autoSaveTask: BukkitTask? = null
    private var isEnabled = false
    private var delay = 60L
    private var modulesToSave = listOf<String>()

    /**
     * 初始化自动保存功能
     */
    fun init(config: ConfigurationSection?) {
        // 停止现有任务
        stop()

        config ?: return

        isEnabled = config.getBoolean("enable", false)
        if (!isEnabled) {
            LoggerUtil.print("&6 * &fAuto save: &cDISABLED")
            return
        }

        delay = config.getLong("delay", 60L).coerceAtLeast(1L) // 至少1秒
        modulesToSave = config.getStringList("modules")

        if (modulesToSave.isEmpty()) {
            LoggerUtil.print("&6 * &fAuto save: &cUNAVAILABLE")
            isEnabled = false
            return
        }

        start()
        LoggerUtil.print("&6 * &fAuto save: &a(${delay}s) &e[${modulesToSave.joinToString(", ")}]")
    }

    /**
     * 启动自动保存任务
     */
    private fun start() {
        if (!isEnabled) return

        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            EfficientSyncBukkit.instance,
            { saveAllPlayersData() },
            delay * 20L,
            delay * 20L
        )
    }

    /**
     * 停止自动保存任务
     */
    fun stop() {
        autoSaveTask?.cancel()
        autoSaveTask = null
    }

    /**
     * 保存所有在线玩家的数据
     */
    private fun saveAllPlayersData() {
        val onlinePlayers = Bukkit.getOnlinePlayers().toList()
        if (onlinePlayers.isEmpty()) return

        EfficientSyncBukkit.instance.logger.info("Auto-saving data for ${onlinePlayers.size} players...")

        onlinePlayers.forEach { player ->
            savePlayerData(player)
        }
    }

    /**
     * 保存单个玩家的数据
     * @param player 要保存数据的玩家
     */
    fun savePlayerData(player: Player) {
        val playerCache = CacheHandler.playerCaches[player.uniqueId] ?: return
        val loadedModules = playerCache.getLoadedModules()
        val repository = RepositoryHandler.repository ?: return

        // 只保存配置中指定的模块且已加载的模块
        modulesToSave.filter { it in loadedModules }.forEach { moduleKey ->
            try {
                val module = ModuleHandler.findByKey(moduleKey) ?: return@forEach
                val data = module.toByteArray(player.uniqueId) ?: return@forEach

                // 在异步线程中执行数据库操作
                Bukkit.getScheduler().runTaskAsynchronously(EfficientSyncBukkit.instance) {
                    try {
                        repository.insert(player.uniqueId, module.uniqueKey, data, SyncState.LOCKED)
                    } catch (e: Exception) {
                        EfficientSyncBukkit.instance.logger.warning("Failed to auto-save $moduleKey for player ${player.name}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                EfficientSyncBukkit.instance.logger.warning("Error preparing auto-save for $moduleKey of player ${player.name}: ${e.message}")
            }
        }
    }

    /**
     * 检查自动保存是否启用
     */
    fun isEnabled(): Boolean = isEnabled
}