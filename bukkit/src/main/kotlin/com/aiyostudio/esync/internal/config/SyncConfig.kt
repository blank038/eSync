package com.aiyostudio.esync.internal.config

import com.aiyostudio.esync.common.repository.impl.MysqlRepositoryImpl
import com.aiyostudio.esync.common.repository.impl.PostgresRepositoryImpl
import com.aiyostudio.esync.internal.api.event.InitModulesEvent
import com.aiyostudio.esync.internal.handler.CacheHandler
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.repository.MysqlVariantRepositoryImpl
import com.aiyostudio.esync.internal.util.LoggerUtil
import com.aiyostudio.supermarketpremium.internal.config.i18n.I18n
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

object SyncConfig {
    var behaviorLock: ConfigurationSection? = null
    var autoUnlock: ConfigurationSection? = null

    fun init() {
        val plugin = EfficientSyncBukkit.instance
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        val config = plugin.config
        CacheHandler.dependModules.clear()
        CacheHandler.dependModules.addAll(config.getStringList("depends").toSet())
        this.behaviorLock = config.getConfigurationSection("sync.behavior-lock")
        this.autoUnlock = config.getConfigurationSection("sync.auto-unlock")
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
        val url = sourceConfig.getString("url")
        val user = sourceConfig.getString("user")
        val password = sourceConfig.getString("password")
        RepositoryHandler.repository = when (type) {
            "mysql" -> MysqlRepositoryImpl(url, user, password)
            "mysql-variant" -> MysqlVariantRepositoryImpl(url, user, password)
            "postgres" -> PostgresRepositoryImpl(url, user, password)
            else -> throw NullPointerException("Failed to initialize repository.")
        }
        RepositoryHandler.repository?.run { this.init() }
        LoggerUtil.print("&6 * &fSync source: &e${RepositoryHandler.repository?.id ?: "NONE"}")
    }
}