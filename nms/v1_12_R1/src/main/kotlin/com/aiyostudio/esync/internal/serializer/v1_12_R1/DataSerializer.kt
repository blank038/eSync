package com.aiyostudio.esync.internal.serializer.v1_12_R1

import com.aiyostudio.esync.internal.serializer.GeneralDataSerializerImpl
import io.netty.buffer.Unpooled
import net.minecraft.server.v1_12_R1.PacketDataSerializer
import net.minecraft.server.v1_12_R1.ServerStatisticManager
import net.minecraft.server.v1_12_R1.Statistic
import net.minecraft.server.v1_12_R1.StatisticManager
import net.minecraft.server.v1_12_R1.StatisticWrapper
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

class DataSerializer : GeneralDataSerializerImpl() {

    override fun deserializerItem(byteArray: ByteArray): ItemStack {
        try {
            return super.deserializerItem(byteArray)
        } catch (_: Exception) {
            val buf = Unpooled.wrappedBuffer(byteArray)
            val packet = PacketDataSerializer(buf)
            return CraftItemStack.asBukkitCopy(packet.k())
        }
    }

    override fun serializerStatistics(player: Player): String? {
        val statisticManager = (player as CraftPlayer).handle.statisticManager
        StatisticManager::class.java.declaredFields.find { it.name == "a" }?.let {
            it.isAccessible = true
            val map = it.get(statisticManager) as ConcurrentHashMap<Statistic, StatisticWrapper>
            return ServerStatisticManager.a(map)
        }
        return null
    }

    override fun deserializerStatistics(player: Player, str: String?): Boolean {
        val statisticManager = (player as CraftPlayer).handle.statisticManager
        val wrapperMap = statisticManager.a(str)
        StatisticManager::class.java.declaredFields.find { it.name == "a" }?.let {
            it.isAccessible = true
            val map = it.get(statisticManager) as ConcurrentHashMap<Statistic, StatisticWrapper>
            map.clear()
            map.putAll(wrapperMap)
        }
        return true
    }
}