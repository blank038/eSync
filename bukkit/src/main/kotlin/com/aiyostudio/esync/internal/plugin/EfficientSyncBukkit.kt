package com.aiyostudio.esync.internal.plugin

import com.aiyostudio.esync.common.EfficientSync
import com.aiyostudio.esync.internal.api.SyncApiBukkitImpl
import com.aiyostudio.esync.internal.command.SyncCommand
import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.listen.PlayerListener
import com.aiyostudio.esync.internal.util.LoggerUtil
import com.aiyostudio.esync.internal.util.SerializerUtil
import com.aystudio.core.bukkit.plugin.AyPlugin
import org.bukkit.Bukkit

class EfficientSyncBukkit : AyPlugin() {

    companion object {
        lateinit var instance: AyPlugin
    }

    override fun onEnable() {
        instance = this
        EfficientSync.api = SyncApiBukkitImpl()
        // print header
        this.consoleLogger.setPrefix("&f[&eeSync&f] &8");
        LoggerUtil.printHeader()
        // initialize module
        SyncConfig.init()
        SerializerUtil.init()
        // register events and commands
        Bukkit.getPluginManager().registerEvents(PlayerListener(), this)
        this.getCommand("esync").executor = SyncCommand()
        // print fotter
        LoggerUtil.printFooter()
    }
}