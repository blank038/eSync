package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.logging.Level

class EnderChestEntity : IEntity {
    val items = mutableMapOf<Int, ItemStack>()

    override fun apply(player: Any): Boolean {
        try {
            if (player is Player) {
                player.enderChest.clear()
                items.forEach { (slot, item) -> player.enderChest.setItem(slot, item) }
                return true
            }
        } catch (e: Exception) {
            EfficientSyncBukkit.instance.logger.log(Level.WARNING, e) {
                "Failed to apply 'ender-chest' data for ${(player as Player).uniqueId}"
            }
        }
        return false
    }
}