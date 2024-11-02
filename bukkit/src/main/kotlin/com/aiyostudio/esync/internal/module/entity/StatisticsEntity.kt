package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.util.SerializerUtil
import org.bukkit.entity.Player
import java.util.logging.Level

class StatisticsEntity : IEntity {
    var data: String? = null

    override fun apply(player: Any): Boolean {
        try {
            if (player !is Player) return false
            return SerializerUtil.deserializerStatistics(player, data)
        } catch (e: Exception) {
            EfficientSyncBukkit.instance.logger.log(Level.WARNING, e) {
                "Failed to apply 'statistics' data for ${(player as Player).uniqueId}"
            }
        }
        return false
    }
}