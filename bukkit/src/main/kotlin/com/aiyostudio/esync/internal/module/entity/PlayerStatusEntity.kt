package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

class PlayerStatusEntity : IEntity {
    val potions = mutableListOf<PotionEffect>()
    var health = 20.0
    var maxHealth = 20.0

    override fun apply(player: Any) {
        if (player is Player) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).baseValue = maxHealth
            player.health = health
            potions.forEach { player.addPotionEffect(it) }
        }
    }
}