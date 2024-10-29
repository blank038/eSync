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
    private val option: ConfigurationSection
) : AbstractModule<PlayerInventoryEntity>() {
    override val uniqueKey: String = "inventory"

    override fun firstLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        if (bytea == null || bytea.isEmpty()) {
            this.caches[uuid] = PlayerInventoryEntity()
        } else {
            this.caches[uuid] = byteaToEntity(bytea)
        }
        return true
    }

    override fun attemptLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        this.firstLoad(uuid, bytea)
        return true
    }

    override fun preLoad(uuid: UUID) {
        if (option.getBoolean("always-clear")) {
            val player = Bukkit.getPlayer(uuid)
            player.inventory.clear()
        }
    }

    override fun find(uuid: UUID): PlayerInventoryEntity? {
        return this.caches[uuid]
    }

    override fun apply(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid)
        this.find(uuid)?.apply(player)
    }

    override fun toByteArray(uuid: UUID): ByteArray? {
        val player = Bukkit.getPlayer(uuid)
        return player?.let {
            val array = 0.until(player.inventory.size)
                .filter { slot -> (player.inventory.getItem(slot)?.type ?: Material.AIR) != Material.AIR }
                .map { slot -> Pair(slot, player.inventory.getItem(slot)) }
                .toTypedArray()
            val buf = Unpooled.buffer()
            buf.writeInt(array.size)
            array.forEach { pair ->
                buf.writeInt(pair.first)
                val bytea = SerializerUtil.serializerItem(pair.second)
                buf.writeInt(bytea.size)
                buf.writeBytes(bytea)
            }
            return buf.array()
        }
    }

    override fun unloadCache(uuid: UUID) {
        this.caches.clear()
    }

    private fun byteaToEntity(bytea: ByteArray): PlayerInventoryEntity {
        val entity = PlayerInventoryEntity()
        val byteBuf = Unpooled.wrappedBuffer(bytea)
        val count = byteBuf.readInt()
        for (i in 0 until count) {
            val slot = byteBuf.readInt()
            val length = byteBuf.readInt()
            val bytes = ByteArray(length)
            bytes.indices.forEach { bytes[it] = byteBuf.readByte() }
            val item = SerializerUtil.deserializerItem(bytes)
            entity.inventory[slot] = item
        }
        return entity
    }
}