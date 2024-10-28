package com.aiyostudio.esync.internal.config

import com.aiyostudio.esync.common.repository.impl.MysqlRepositoryImpl
import com.aiyostudio.esync.internal.api.event.InitModulesEvent
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.util.LoggerUtil
import com.aiyostudio.supermarketpremium.internal.config.i18n.I18n
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration

object SyncConfig {
    private val dependModules = mutableListOf<String>()

    fun init() {
        val plugin = EfficientSyncBukkit.instance
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        val config = plugin.config
        dependModules.clear()
        dependModules.addAll(config.getStringList("depends"))
        // initialize i18n
        I18n(config.getString("language"))
        // register modules
        this.registerModules(config)
        // initialize repository
        this.initRepository(config)
    }

    private fun registerModules(config: FileConfiguration) {
        ModuleHandler.unloadAllModule()
        config.getConfigurationSection("modules")?.getKeys(false)?.forEach {
            val option = config.getConfigurationSection("modules.$it")
            if (option?.getBoolean("enable") == true) {
                val module = ModuleHandler.createDefaultModule(it, option) ?: return@forEach
                ModuleHandler.register(module)
            }
        }
        val event = InitModulesEvent()
        Bukkit.getPluginManager().callEvent(event)
    }

    private fun initRepository(config: FileConfiguration) {
        RepositoryHandler.repository?.disable()
        val type = config.getString("sync.type").lowercase()
        val sourceConfig = config.getConfigurationSection("sync.sources.$type")
        if (sourceConfig == null) {
            LoggerUtil.print("&cFailed to initialize repository, check the config.yml file.", true)
            return
        }
        RepositoryHandler.repository = when(type) {
            "mysql" -> MysqlRepositoryImpl(
                sourceConfig.getString("url"),
                sourceConfig.getString("user"),
                sourceConfig.getString("password")
            )
            else -> throw NullPointerException("Failed to initialize repository.")
        }
    }
}