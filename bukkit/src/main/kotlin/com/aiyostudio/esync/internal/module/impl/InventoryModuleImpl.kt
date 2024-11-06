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
        this.convertCache(uuid, bytea)
        if (option.contains("first-login-commands")) {
            this.caches[uuid]?.commands?.addAll(option.getStringList("first-login-commands"))
        }
        return true
    }

    override fun attemptLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        this.convertCache(uuid, bytea)
        return true
    }

    private fun convertCache(uuid: UUID, bytea: ByteArray?) {
        if (bytea == null || bytea.isEmpty()) {
            this.caches[uuid] = PlayerInventoryEntity()
        } else {
            this.caches[uuid] = wrapper(bytea)
        }
    }

    override fun preLoad(uuid: UUID) {
        if (option.getBoolean("always-clear")) {
            val player = Bukkit.getPlayer(uuid)
            player?.takeIf { it.isOnline }?.run { inventory.clear() }
        }
    }

    override fun apply(uuid: UUID): Boolean {
        val player = Bukkit.getPlayer(uuid)
        return this.find(uuid)?.apply(player) ?: false
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

    override fun wrapper(bytea: ByteArray): PlayerInventoryEntity {
        val entity = PlayerInventoryEntity()
        val byteBuf = Unpooled.wrappedBuffer(bytea)
        val count = byteBuf.readInt()
        for (i in 0 until count) {
            val slot = byteBuf.readInt()
            val length = byteBuf.readInt()
            val bytes = ByteArray(length)
            byteBuf.readBytes(bytes)
            val item = SerializerUtil.deserializerItem(bytes)
            entity.inventory[slot] = item
        }
        return entity
    }
}