package com.aiyostudio.esyncpixelmon

import com.aiyostudio.esync.internal.api.event.InitModulesEvent
import com.aiyostudio.esync.internal.handler.CacheHandler
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esyncpixelmon.module.PixelmonModuleImpl
import com.aystudio.core.bukkit.plugin.AyPlugin
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class SyncPixelmon : AyPlugin(), Listener {

    companion object {
        lateinit var instance: SyncPixelmon
    }

    override fun onEnable() {
        instance = this
        this.loadConfig()
        Bukkit.getPluginManager().registerEvents(this, this)
    }

    private fun loadConfig() {
        this.saveDefaultConfig()
        this.reloadConfig()
    }

    @EventHandler
    fun onInit(event: InitModulesEvent) {
        if (!this.config.getBoolean("option.enable")) {
            return
        }
        val module = PixelmonModuleImpl()
        if (!ModuleHandler.register(module)) {
            return
        }
        this.config.getBoolean("depend")
            .takeIf { it }
            ?.let { CacheHandler.dependModules.add("pixelmon") }
    }
}