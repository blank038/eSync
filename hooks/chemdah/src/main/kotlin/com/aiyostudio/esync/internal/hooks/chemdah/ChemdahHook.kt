package com.aiyostudio.esync.internal.hooks.chemdah

import com.aiyostudio.esync.internal.api.event.PlayerModuleEvent
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import ink.ptms.chemdah.core.database.ChemdahDatabase
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ChemdahHook : Listener {

    init {
        if (EfficientSyncBukkit.instance.config.getBoolean("hooks.chemdah")) {
            ChemdahDatabase.isLoadInJoinEvent = false
        }
        Bukkit.getPluginManager().registerEvents(this, EfficientSyncBukkit.instance)
    }

    @EventHandler
    fun onDependLoaded(event: PlayerModuleEvent.DependLoaded) {
        if (EfficientSyncBukkit.instance.config.getBoolean("hooks.chemdah")) {
            ChemdahDatabase.loadProfile(event.player)
        }
    }
}