package com.aiyostudio.esync.internal.module.impl

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esync.internal.module.entity.PlayerStatusEntity
import io.netty.buffer.Unpooled
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.potion.PotionEffect
import java.util.*

class PlayerStatusModuleImpl(
    private val option: ConfigurationSection
) : AbstractModule<PlayerStatusEntity>() {
    override val uniqueKey: String = "player-status"

    override fun firstLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        if (bytea == null || bytea.isEmpty()) {
            this.caches[uuid] = PlayerStatusEntity()
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
            player?.takeIf { it.isOnline }?.run {
                activePotionEffects.stream().map { it.type }.forEach { player.removePotionEffect(it) }
            }
        }
    }

    override fun apply(uuid: UUID): Boolean {
        val player = Bukkit.getPlayer(uuid)
        return this.find(uuid)?.apply(player) ?: false
    }

    override fun toByteArray(uuid: UUID): ByteArray? {
        val player = Bukkit.getPlayer(uuid)
        return player?.let {
            val buf = Unpooled.buffer()
            try {
                buf.writeDouble(it.health)
                buf.writeDouble(it.getAttribute(Attribute.GENERIC_MAX_HEALTH).baseValue)
                val collection = listOf(*it.activePotionEffects.toTypedArray())
                buf.writeInt(collection.size)
                collection.forEach { potion ->
                    val section = YamlConfiguration()
                    section.set("potion", potion)
                    val str = section.saveToString()
                    val bytea = str.toByteArray(Charsets.UTF_8)
                    buf.writeInt(bytea.size)
                    buf.writeBytes(bytea)
                }
                buf.array()
            } finally {
                buf.release()
            }
        }
    }

    override fun wrapper(bytea: ByteArray): PlayerStatusEntity {
        val result = PlayerStatusEntity()
        val buf = Unpooled.wrappedBuffer(bytea)
        try {
            result.health = buf.readDouble()
            result.maxHealth = buf.readDouble()
            val size = buf.readInt()
            for (i in 0 until size) {
                val length = buf.readInt()
                val byteArray = ByteArray(length)
                buf.readBytes(byteArray)
                val str = String(byteArray, Charsets.UTF_8)
                val yaml = YamlConfiguration()
                yaml.loadFromString(str)
                val potion = yaml.get("potion") as PotionEffect
                result.potions.add(potion)
            }
            return result
        } finally {
            buf.release()
        }
    }
}