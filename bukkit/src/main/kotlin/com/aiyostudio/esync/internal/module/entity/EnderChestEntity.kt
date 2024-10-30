package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class EnderChestEntity : IEntity {
    val items = mutableMapOf<Int, ItemStack>()

    override fun apply(player: Any): Boolean {
        if (player is Player) {
            player.enderChest.clear()
            items.forEach { (slot, item) -> player.enderChest.setItem(slot, item) }
            return true
        }
        return false
    }
}