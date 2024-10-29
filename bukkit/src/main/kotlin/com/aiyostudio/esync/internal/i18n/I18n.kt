package com.aiyostudio.supermarketpremium.internal.config.i18n

import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aystudio.core.bukkit.util.common.CommonUtil
import com.google.common.collect.Lists
import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

class I18n(language: String) {
    private val stringOptions: MutableMap<String?, String> = HashMap()
    private val arrayOptions: MutableMap<String, List<String>> = HashMap()
    private lateinit var language: String
    private lateinit var header: String


    init {
        this.init(language)
    }

    private fun init(language: String) {
        instance = this
        this.language = language
        stringOptions.clear()
        arrayOptions.clear()
        val folder = File(EfficientSyncBukkit.instance.dataFolder, "language")
        if (!folder.exists()) {
            folder.mkdir()
            for (lang in LANGUAGES) {
                val tar = File(folder, lang)
                CommonUtil.outputFileTool(EfficientSyncBukkit.instance.getResource("language/$lang"), tar)
            }
        }
        // 读取语言配置文件
        var file = File(folder, "$language.yml")
        if (!file.exists()) {
            file = File(folder, "zh_CN.yml")
        }
        val data: FileConfiguration = YamlConfiguration.loadConfiguration(file)
        data.getKeys(true).forEach { parseOptions(data, it) }
        this.header = "prefix"
    }

    private fun parseOptions(section: ConfigurationSection, key: String) {
        when {
            section.isString(key) -> {
                stringOptions[key] = ChatColor.translateAlternateColorCodes('&', section.getString(key))
            }

            section.isList(key) -> {
                arrayOptions[key] = section.getStringList(key).apply {
                    this.replaceAll { ChatColor.translateAlternateColorCodes('&', it) }
                }
            }
        }
    }

    companion object {
        private val LANGUAGES = arrayOf("zh_CN.yml")
        lateinit var instance: I18n

        fun getStrAndHeader(key: String): String {
            return getOption(instance.header, key)
        }

        fun getArrayOption(key: String): List<String> {
            if (instance.arrayOptions.containsKey(key)) {
                return ArrayList(instance.arrayOptions[key]!!)
            }
            return Lists.newArrayList()
        }

        fun getOption(key: String): String {
            return instance.stringOptions.getOrDefault(key, "")
        }

        private fun getOption(header: String, key: String): String {
            return this.getOption(header) + this.getOption(key)
        }
    }
}