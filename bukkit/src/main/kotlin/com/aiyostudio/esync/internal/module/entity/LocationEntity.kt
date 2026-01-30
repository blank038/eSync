package com.aiyostudio.esync.internal.module.entity

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.i18n.I18n
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.logging.Level

class LocationEntity : IEntity {
    var worldName: String = ""
    var x: Double = 0.0
    var y: Double = 0.0
    var z: Double = 0.0
    var yaw: Float = 0f
    var pitch: Float = 0f

    override fun apply(player: Any): Boolean {
        if (player !is Player) {
            return false
        }
        try {
            // 如果世界名为空（首次加载），直接返回成功，不执行传送
            if (worldName.isEmpty()) {
                EfficientSyncBukkit.instance.logger.info(
                    "No saved location for player ${player.name}, skipping teleport"
                )
                return true
            }
            val world = Bukkit.getWorld(worldName) ?: let {
                EfficientSyncBukkit.instance.logger.warning(
                    "World '$worldName' not found for player ${player.name}'s saved location"
                )

                // 发送传送失败消息
                if (SyncConfig.isChatMessageEnabled()) {
                    player.sendMessage(I18n.getStrAndHeader("sync.location.teleport-failed"))
                }
                // 返回 true 以确保模块被标记为已加载，这样新位置才能被保存
                return true
            }
            // 延迟传送玩家
            Bukkit.getScheduler().runTaskLater(EfficientSyncBukkit.instance, {
                val location = Location(world, x, y, z, yaw, pitch)
                player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
            }, 20L * EfficientSyncBukkit.instance.config.getInt("modules.location.delay"))

            // 输出传送日志
            EfficientSyncBukkit.instance.logger.info(
                "Player ${player.name} teleported to $worldName (${String.format("%.2f", x)}, ${String.format("%.2f", y)}, ${String.format("%.2f", z)})"
            )

            // 发送传送成功消息
            if (SyncConfig.isChatMessageEnabled()) {
                player.sendMessage(I18n.getStrAndHeader("sync.location.teleport-success"))
            }
        } catch (e: Exception) {
            EfficientSyncBukkit.instance.logger.log(Level.WARNING, e) {
                "Failed to apply 'location' data for ${player.uniqueId}"
            }
            if (SyncConfig.isChatMessageEnabled()) {
                player.sendMessage(I18n.getStrAndHeader("sync.location.teleport-failed"))
            }
        }
        return true
    }
}
