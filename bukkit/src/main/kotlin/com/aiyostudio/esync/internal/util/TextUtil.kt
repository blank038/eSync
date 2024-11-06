package com.aiyostudio.esync.internal.util

import org.bukkit.ChatColor
import org.bukkit.configuration.ConfigurationSection

object TextUtil {

    fun String.colorify(): String {
        return ChatColor.translateAlternateColorCodes('&', this)
    }

    fun ConfigurationSection.getColorifyStringList(key: String): MutableList<String> {
        return this.getStringList(key).apply {
            this.replaceAll { ChatColor.translateAlternateColorCodes('&', it) }
        }
    }
}