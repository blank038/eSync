package com.aiyostudio.esync.internal.listen

import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.transaction.SyncTransaction
import com.aiyostudio.supermarketpremium.internal.config.i18n.I18n
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener : Listener {
    private val plugin = EfficientSyncBukkit.instance

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        Bukkit.getScheduler().runTaskLater(this.plugin, {
            if (player?.isOnline == false) {
                return@runTaskLater
            }
            SyncTransaction(
                player.uniqueId,
                plugin.config.getStringList("depends"),
                { player.sendMessage(I18n.getStrAndHeader("sync.success")) },
                { player.sendMessage(I18n.getStrAndHeader("sync.failed")) }
            ).start()
        }, 10L)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {

    }
}