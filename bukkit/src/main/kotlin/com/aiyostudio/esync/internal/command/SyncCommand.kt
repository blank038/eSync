package com.aiyostudio.esync.internal.command

import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.i18n.I18n
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class SyncCommand : CommandExecutor {
    private val plugin = EfficientSyncBukkit.instance

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, params: Array<out String>): Boolean {
        if (!sender.hasPermission("esync.admin") || params.size < 1) return false
        when (params[0].lowercase()) {
            "kickall" -> this.kickAll(sender)
            "reload" -> this.reload(sender)
        }
        return false
    }

    private fun kickAll(sender: CommandSender) {
        Bukkit.getOnlinePlayers().forEach { it.kickPlayer(I18n.getOption("kick-all.reason")) }
        sender.sendMessage(I18n.getStrAndHeader("kick-all.message"))
    }

    private fun reload(sender: CommandSender) {
        SyncConfig.init()
        sender.sendMessage(I18n.getStrAndHeader("reload"))
    }
}