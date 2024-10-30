package com.aiyostudio.esync.internal.module.impl

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esync.internal.module.entity.EnderChestEntity
import com.aiyostudio.esync.internal.util.SerializerUtil
import io.netty.buffer.Unpooled
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import java.util.*

class EnderChestModuleImpl(
    private val option: ConfigurationSection
) : AbstractModule<EnderChestEntity>() {
    override val uniqueKey: String = "ender-chest"

    override fun firstLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        if (bytea == null || bytea.isEmpty()) {
            this.caches[uuid] = EnderChestEntity()
        } else {
            this.caches[uuid] = wrapper(bytea)
        }
        return true
    }

    override fun attemptLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        return firstLoad(uuid, bytea)
    }

    override fun preLoad(uuid: UUID) {
        if (option.getBoolean("always-clear")) {
            val player = Bukkit.getPlayer(uuid)
            player.enderChest.clear()
        }
    }

    override fun apply(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid)
        this.find(uuid)?.apply(player)
    }

    override fun toByteArray(uuid: UUID): ByteArray? {
        val player = Bukkit.getPlayer(uuid)
        return player?.let {
            val array = 0.until(player.enderChest.size)
                .filter { slot -> (player.enderChest.getItem(slot)?.type ?: Material.AIR) != Material.AIR }
                .map { slot -> Pair(slot, player.enderChest.getItem(slot)) }
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

    override fun wrapper(bytea: ByteArray): EnderChestEntity {
        val entity = EnderChestEntity()
        val byteBuf = Unpooled.wrappedBuffer(bytea)
        val count = byteBuf.readInt()
        for (i in 0 until count) {
            val slot = byteBuf.readInt()
            val length = byteBuf.readInt()
            val bytes = byteBuf.readBytes(length).array()
            val item = SerializerUtil.deserializerItem(bytes)
            entity.items[slot] = item
        }
        return entity
    }
}