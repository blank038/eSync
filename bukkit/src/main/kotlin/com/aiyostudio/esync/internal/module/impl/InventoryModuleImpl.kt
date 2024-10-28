package com.aiyostudio.esync.internal.module.impl

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esync.internal.module.entity.PlayerInventoryEntity
import com.aiyostudio.esync.internal.util.SerializerUtil
import io.netty.buffer.Unpooled
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import java.util.*


class InventoryModuleImpl(
    option: ConfigurationSection
) : AbstractModule<PlayerInventoryEntity>() {
    override val uniqueKey: String = "inventory"

    override fun attemptLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        if (bytea == null) {
            this.caches[uuid] = PlayerInventoryEntity()
            return true
        }
        val entity = PlayerInventoryEntity()
        val byteBuf = Unpooled.wrappedBuffer(bytea)
        val count = byteBuf.readInt()
        for (i in 0 until count) {
            val slot = byteBuf.readInt()
            val length = byteBuf.readableBytes()
            val bytes = ByteArray(length)
            bytes.indices.forEach { bytes[it] = byteBuf.readByte() }
            val item = SerializerUtil.deserializerItem(bytes)
            entity.inventory[slot] = item
        }
        this.caches[uuid] = entity
        return true
    }

    override fun find(uuid: UUID): PlayerInventoryEntity? {
        return this.caches.remove(uuid)
    }

    override fun toByteArray(uuid: UUID): ByteArray? {
        val player = Bukkit.getPlayer(uuid)
        return player?.let {
            val array = 0.until(player.inventory.size)
                .filter { slot ->
                    val item = player.inventory.getItem(slot)
                    return@filter item != null && item.type != Material.AIR
                }
                .map { slot -> Pair(slot, player.inventory.getItem(slot)) }
                .toTypedArray()
            val buf = Unpooled.buffer()
            buf.writeInt(array.size)
            array.forEach { pair ->
                buf.writeInt(pair.first)
                buf.writeBytes(SerializerUtil.serializerItem(pair.second))
            }
            return buf.array()
        }
    }

    override fun unloadCache(uuid: UUID) {
        this.caches.clear()
    }
}