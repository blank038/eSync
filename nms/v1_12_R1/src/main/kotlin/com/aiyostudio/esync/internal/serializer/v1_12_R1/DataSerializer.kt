package com.aiyostudio.esync.internal.serializer.v1_12_R1

import com.aiyostudio.esync.internal.serializer.IDataSerializer
import io.netty.buffer.Unpooled
import net.minecraft.server.v1_12_R1.PacketDataSerializer
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack
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
        return CraftItemStack.asBukkitCopy(packet.k())
    }
}