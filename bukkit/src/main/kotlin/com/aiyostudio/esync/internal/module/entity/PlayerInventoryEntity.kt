package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

class PlayerInventoryEntity : IEntity {
    val inventory = mutableMapOf<Int, ItemStack>()
    val commands = mutableListOf<String>()

    override fun apply(player: Any): Boolean {
        try {
            if (player is Player) {
                player.inventory.clear()
                inventory.forEach { (slot, item) -> player.inventory.setItem(slot, item) }
                // Execute commands on first login.
                commands.forEach {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.replace("%player%", player.name))
                }
                return true
            }
        } catch (e: Exception) {
            EfficientSyncBukkit.instance.logger.log(Level.WARNING, e) {
                "Failed to apply 'inventory' data for ${(player as Player).uniqueId}"
            }
        }
        return false
    }
}