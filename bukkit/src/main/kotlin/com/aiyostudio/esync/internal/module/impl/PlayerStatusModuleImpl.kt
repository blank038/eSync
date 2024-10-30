package com.aiyostudio.esync.internal.module.impl

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esync.internal.module.entity.PlayerStatusEntity
import io.netty.buffer.Unpooled
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
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
            player.activePotionEffects.stream().map { it.type }.forEach { player.removePotionEffect(it) }
        }
    }

    override fun apply(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid)
        this.find(uuid)?.apply(player)
    }

    override fun toByteArray(uuid: UUID): ByteArray? {
        val player = Bukkit.getPlayer(uuid)
        return player?.let {
            val buf = Unpooled.buffer()
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
        }
    }

    override fun wrapper(bytea: ByteArray): PlayerStatusEntity {
        val result = PlayerStatusEntity()
        val buf = Unpooled.wrappedBuffer(bytea)
        result.health = buf.readDouble()
        result.maxHealth = buf.readDouble()
        val size = buf.readInt()
        for (i in 0 until size) {
            val length = buf.readInt()
            val byteArray = buf.readBytes(length).array()
            val str = String(byteArray, Charsets.UTF_8)
            val yaml = YamlConfiguration()
            yaml.loadFromString(str)
            val map = yaml.getConfigurationSection("potion").getValues(false)
            val option = ConfigurationSerialization.deserializeObject(map, PotionEffect::class.java) as PotionEffect
            result.potions.add(option)
        }
        return result
    }
}