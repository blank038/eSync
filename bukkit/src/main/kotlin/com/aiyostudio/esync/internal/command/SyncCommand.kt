package com.aiyostudio.esync.internal.command

import com.aiyostudio.esync.common.repository.IRepository
import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import com.aiyostudio.esync.internal.i18n.I18n
import com.aiyostudio.esync.internal.module.entity.EnderChestEntity
import com.aiyostudio.esync.internal.module.entity.PlayerInventoryEntity
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.util.LoggerUtil
import com.aiyostudio.esync.internal.view.EnderChestView
import com.aiyostudio.esync.internal.view.InventoryView
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.lang.Exception
import java.util.UUID

class SyncCommand : CommandExecutor {
    private val plugin = EfficientSyncBukkit.instance

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, params: Array<out String>): Boolean {
        if (!sender.hasPermission("esync.admin")) return false
        if (params.isEmpty()) {
            I18n.getArrayOption("commands").forEach { sender.sendMessage(it.replace("%c", label)) }
            return false
        }
        when (params[0].lowercase()) {
            "kickall" -> this.kickAll(sender)
            "reload" -> this.reload(sender)
            "inv" -> this.view("inventory", sender, params)
            "ender" -> this.view("enderchest", sender, params)
        }
        return false
    }

    private fun kickAll(sender: CommandSender) {
        Bukkit.getOnlinePlayers().forEach { it.kickPlayer(I18n.getOption("kick-all.reason")) }
        sender.sendMessage(I18n.getStrAndHeader("kick-all.message"))
    }

    private fun reload(sender: CommandSender) {
        LoggerUtil.printHeader()
        SyncConfig.init()
        LoggerUtil.printFooter()
        sender.sendMessage(I18n.getStrAndHeader("reload"))
    }

    private fun view(type: String, sender: CommandSender, params: Array<out String>) {
        if (sender !is Player) return
        if (params.size == 1) {
            sender.sendMessage(I18n.getStrAndHeader("params.enter-uuid"))
            return
        }
        val repo = RepositoryHandler.repository ?: let {
            sender.sendMessage(I18n.getStrAndHeader("repo-failure"))
            return
        }
        try {
            val uuid = UUID.fromString(params[1])
            when (type) {
                "inventory" -> this.viewInv(uuid, sender, repo)
                "enderchest" -> this.viewEnderChest(uuid, sender, repo)
            }
        } catch (e: Exception) {
            sender.sendMessage(I18n.getStrAndHeader("params.uuid-failure"))
        }
    }

    private fun viewInv(target: UUID, sender: Player, repo: IRepository) {
        val module = ModuleHandler.findByKey("inventory") ?: let {
            sender.sendMessage(I18n.getStrAndHeader("module-failure"))
            return
        }
        val result = repo.queryData(target, "inventory") ?: let {
            sender.sendMessage(I18n.getStrAndHeader("empty-data"))
            return
        }
        if (result.isEmpty()) {
            sender.sendMessage(I18n.getStrAndHeader("empty-data"))
            return
        }
        val entity = module.wrapper(result) as PlayerInventoryEntity
        InventoryView(sender, entity)
    }

    private fun viewEnderChest(target: UUID, sender: Player, repo: IRepository) {
        val module = ModuleHandler.findByKey("ender-chest") ?: let {
            sender.sendMessage(I18n.getStrAndHeader("module-failure"))
            return
        }
        val result = repo.queryData(target, "ender-chest") ?: let {
            sender.sendMessage(I18n.getStrAndHeader("empty-data"))
            return
        }
        if (result.isEmpty()) {
            sender.sendMessage(I18n.getStrAndHeader("empty-data"))
            return
        }
        val entity = module.wrapper(result) as EnderChestEntity
        EnderChestView(sender, entity)
    }
}