package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.Statistic
import org.bukkit.entity.Player
import java.util.logging.Level

class StatisticsEntity : IEntity {
    val statistics = mutableMapOf<Statistic, Int>()

    override fun apply(player: Any): Boolean {
        try {
            if (player !is Player) return false
            statistics.forEach { (k, v) -> player.setStatistic(k, v) }
        } catch (e: Exception) {
            EfficientSyncBukkit.instance.logger.log(Level.WARNING, e) {
                "Failed to apply 'statistics' data for ${(player as Player).uniqueId}"
            }
        }
        return false
    }
}