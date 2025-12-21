package com.aiyostudio.esync.internal.listen

import com.aiyostudio.esync.internal.cache.PlayerCache
import com.aiyostudio.esync.internal.command.SyncCommand
import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.handler.AutoSaveHandler
import com.aiyostudio.esync.internal.handler.CacheHandler
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.i18n.I18n
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.transaction.SyncTransaction
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent

class PlayerListener : Listener {
    private val plugin = EfficientSyncBukkit.instance

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val isMoveLocked = SyncConfig.behaviorLock?.getBoolean("move", true) ?: true
        val isPlayerNotLoaded = CacheHandler.playerCaches[event.player.uniqueId]?.dependLoaded == false
        if (isMoveLocked && isPlayerNotLoaded) {
            event.isCancelled = true
            event.to = event.from
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val isChatLocked = SyncConfig.behaviorLock?.getBoolean("locked", true) ?: true
        val isPlayerNotLoaded = CacheHandler.playerCaches[event.player.uniqueId]?.dependLoaded == false
        if (isChatLocked && isPlayerNotLoaded) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val isInvClickLocked = SyncConfig.behaviorLock?.getBoolean("inv-click", true) ?: true
        val isPlayerNotLoaded = CacheHandler.playerCaches[event.whoClicked.uniqueId]?.dependLoaded == false
        if (isInvClickLocked && isPlayerNotLoaded) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        val isDropLocked = SyncConfig.behaviorLock?.getBoolean("drop", true) ?: true
        val isPlayerNotLoaded = CacheHandler.playerCaches[event.player.uniqueId]?.dependLoaded == false
        if (isDropLocked && isPlayerNotLoaded) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val isInteractLocked = SyncConfig.behaviorLock?.getBoolean("interact", true) ?: true
        val isPlayerNotLoaded = CacheHandler.playerCaches[event.player.uniqueId]?.dependLoaded == false
        if (isInteractLocked && isPlayerNotLoaded) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPickupItem(event: EntityPickupItemEvent) {
        if (event.entity is Player) {
            val isPickupLocked = SyncConfig.behaviorLock?.getBoolean("pickup", true) ?: true
            val isPlayerNotLoaded = CacheHandler.playerCaches[event.entity.uniqueId]?.dependLoaded == false
            if (isPickupLocked && isPlayerNotLoaded) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val isPickupLocked = SyncConfig.behaviorLock?.getBoolean("command.lock", true) ?: true
        if (!isPickupLocked) {
            return
        }
        val bypassCommands = SyncConfig.behaviorLock?.getStringList("command.bypass-commands") ?: emptyList()
        if (bypassCommands.any { event.message.startsWith(it) }) {
            return
        }
        val isPlayerNotLoaded = CacheHandler.playerCaches[event.player.uniqueId]?.dependLoaded == false
        if (isPlayerNotLoaded) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        // 检查是否处于维护模式
        if (SyncCommand.isInMaintainMode()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, I18n.getOption("save-and-stop.kick-reason"))
            return
        }
    }

    @EventHandler
    fun onLogin(event: PlayerLoginEvent) {
        // 再次检查维护模式，以防PreLogin事件被其他插件修改
        if (SyncCommand.isInMaintainMode()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, I18n.getOption("save-and-stop.kick-reason"))
            return
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        CacheHandler.playerCaches[player.uniqueId] = PlayerCache()
        Bukkit.getScheduler().runTaskLater(this.plugin, {
            if (player?.isOnline == false) {
                return@runTaskLater
            }
            // load depend modules
            SyncTransaction(
                player.uniqueId,
                CacheHandler.dependModules,
                { player.sendMessage(I18n.getStrAndHeader("sync.success")) },
                { player.sendMessage(I18n.getStrAndHeader("sync.failed")) }
            ).start()
            // load other modules
            ModuleHandler.getAllModules(CacheHandler.dependModules).forEach {
                SyncTransaction(player.uniqueId, listOf(it)).start()
            }
        }, 10L)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        CacheHandler.removeAndSaved(event.player)
    }

    @EventHandler
    fun onPluginUnload(event: PluginDisableEvent) {
        if (event.plugin != EfficientSyncBukkit.instance) {
            return
        }
        // 停止自动保存任务
        AutoSaveHandler.stop()
        // 保存所有在线玩家的数据
        Bukkit.getOnlinePlayers().forEach(CacheHandler::removeAndSaved)
    }
}