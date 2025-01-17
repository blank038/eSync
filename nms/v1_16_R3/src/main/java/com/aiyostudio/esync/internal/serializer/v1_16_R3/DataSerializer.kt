package com.aiyostudio.esync.internal.serializer.v1_16_R3

import com.aiyostudio.esync.internal.serializer.IDataSerializer
import io.netty.buffer.Unpooled
import net.minecraft.server.v1_16_R3.PacketDataSerializer
import net.minecraft.server.v1_16_R3.ServerStatisticManager
import net.minecraft.server.v1_16_R3.StatisticManager
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_16_R3.CraftServer
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DataSerializer : IDataSerializer {

    override fun serializerItem(itemStack: ItemStack): ByteArray {
        val buf = Unpooled.buffer()
        val packet = PacketDataSerializer(buf)
        packet.a(CraftItemStack.asNMSCopy(itemStack))
        return packet.array()
    }

    override fun deserializerItem(byteArray: ByteArray): ItemStack {
        val buf = Unpooled.wrappedBuffer(byteArray)
        val packet = PacketDataSerializer(buf)
        return CraftItemStack.asBukkitCopy(packet.n())
    }

    override fun serializerStatistics(player: Player): String? {
        val statisticManager = (player as CraftPlayer).handle.statisticManager
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
        val statisticManager = (player as CraftPlayer).handle.statisticManager
        statisticManager.a((Bukkit.getServer() as CraftServer).server.dataFixer, str)
        return true
    }
}