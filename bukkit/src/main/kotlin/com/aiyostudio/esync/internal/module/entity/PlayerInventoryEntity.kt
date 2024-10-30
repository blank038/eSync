package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class PlayerInventoryEntity : IEntity {
    val inventory = mutableMapOf<Int, ItemStack>()

    override fun apply(player: Any): Boolean {
        if (player is Player) {
            player.inventory.clear()
            inventory.forEach { (slot, item) -> player.inventory.setItem(slot, item) }
            return true
        }
        return false
    }
}