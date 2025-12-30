package com.aiyostudio.esync.internal.serializer.v1_21_R1

import com.aiyostudio.esync.internal.serializer.GeneralDataSerializerImpl
import net.minecraft.stats.ServerStatisticManager
import net.minecraft.stats.StatisticManager
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_21_R1.CraftServer
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer
import org.bukkit.entity.Player


class DataSerializer : GeneralDataSerializerImpl() {

    override fun serializerStatistics(player: Player): String? {
        val statisticManager = (player as CraftPlayer).handle.I()
        StatisticManager::class.java.declaredFields.find { it.name == "a" }?.let {
            it.isAccessible = true
            // reflect call
            ServerStatisticManager::class.java.declaredMethods
                .find { method -> method.name == "b" && method.parameterCount == 0 }
                ?.let { method ->
                    method.isAccessible = true
                    val result = method.invoke(statisticManager) as String
                    return result
                }
        }
        return null
    }

    override fun deserializerStatistics(player: Player, str: String?): Boolean {
        val statisticManager = (player as CraftPlayer).handle.I()
        statisticManager.a((Bukkit.getServer() as CraftServer).server.aD(), str)
        return true
    }
}