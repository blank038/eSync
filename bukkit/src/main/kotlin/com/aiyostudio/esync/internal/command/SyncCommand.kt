package com.aiyostudio.esync.internal.command

import com.aiyostudio.esync.common.repository.IRepository
import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.handler.AutoSaveHandler
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
            "autosave" -> this.autoSave(sender, params)
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

    private fun autoSave(sender: CommandSender, params: Array<out String>) {
        if (params.size == 1) {
            // 手动触发所有玩家数据保存
            if (AutoSaveHandler.isEnabled()) {
                Bukkit.getOnlinePlayers().forEach { player ->
                    AutoSaveHandler.savePlayerData(player)
                }
                sender.sendMessage("§a已手动触发所有在线玩家的数据保存")
            } else {
                sender.sendMessage("§c自动保存功能未启用，请检查配置文件")
            }
        } else if (params.size == 2) {
            // 保存指定玩家的数据
            val targetPlayer = Bukkit.getPlayer(params[1])
            if (targetPlayer == null) {
                sender.sendMessage("§c玩家 " + params[1] + " 不在线")
                return
            }
            
            if (AutoSaveHandler.isEnabled()) {
                AutoSaveHandler.savePlayerData(targetPlayer)
                sender.sendMessage("§a已保存玩家 " + targetPlayer.name + " 的数据")
            } else {
                sender.sendMessage("§c自动保存功能未启用，请检查配置文件")
            }
        } else {
            sender.sendMessage("§c用法: /esync autosave [玩家名]")
        }
    }
}