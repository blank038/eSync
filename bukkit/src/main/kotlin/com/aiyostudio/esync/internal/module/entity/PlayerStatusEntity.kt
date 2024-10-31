package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import java.util.logging.Level

class PlayerStatusEntity : IEntity {
    val potions = mutableListOf<PotionEffect>()
    var health = 20.0
    var maxHealth = 20.0

    override fun apply(player: Any): Boolean {
        try {
            if (player is Player) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).baseValue = maxHealth
                player.health = health
                potions.forEach { player.addPotionEffect(it) }
                return true
            }
        } catch (e: Exception) {
            EfficientSyncBukkit.instance.logger.log(Level.WARNING, e) {
                "Failed to apply 'player-status' data for ${(player as Player).uniqueId}"
            }
        }
        return false
    }
}