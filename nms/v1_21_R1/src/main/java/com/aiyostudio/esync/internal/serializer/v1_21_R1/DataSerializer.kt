package com.aiyostudio.esync.internal.serializer.v1_21_R1

import com.aiyostudio.esync.internal.serializer.GeneralDataSerializerImpl
import net.minecraft.stats.ServerStatsCounter
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player


class DataSerializer : GeneralDataSerializerImpl() {

//    override fun serializerItem(itemStack: org.bukkit.inventory.ItemStack): ByteArray {
//        val byteArrayStream = ByteArrayOutputStream()
//        BukkitObjectOutputStream(byteArrayStream).use { it.writeObject(itemStack) }
//        return byteArrayStream.toByteArray()
//    }
//
//    override fun deserializerItem(byteArray: ByteArray): org.bukkit.inventory.ItemStack {
//        if (byteArray.isEmpty()) {
//            return org.bukkit.inventory.ItemStack(Material.AIR)
//        }
//        return try {
//            val byteArrayStream = ByteArrayInputStream(byteArray)
//            BukkitObjectInputStream(byteArrayStream).use {
//                it.readObject() as org.bukkit.inventory.ItemStack
//            }
//        } catch (_: Exception) {
//            org.bukkit.inventory.ItemStack(Material.AIR)
//        }
//    }

    override fun serializerStatistics(player: Player): String {
        val stats = (player as CraftPlayer).handle.stats
        val method = ServerStatsCounter::class.java.getDeclaredMethod("method_14911")
        method.isAccessible = true
        return method.invoke(stats) as String
    }

    override fun deserializerStatistics(player: Player, str: String?): Boolean {
        val stats = (player as CraftPlayer).handle.stats
        stats.parseLocal((Bukkit.getServer() as CraftServer).server.fixerUpper, str)
        return true
    }
}