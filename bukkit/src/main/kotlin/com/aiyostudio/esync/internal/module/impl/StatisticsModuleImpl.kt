package com.aiyostudio.esync.internal.module.impl

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esync.internal.module.entity.StatisticsEntity
import io.netty.buffer.Unpooled
import org.bukkit.Bukkit
import org.bukkit.Statistic
import org.bukkit.configuration.ConfigurationSection
import java.util.*

class StatisticsModuleImpl(
    private val option: ConfigurationSection
) : AbstractModule<StatisticsEntity>() {
    override val uniqueKey: String = "statistics"

    override fun firstLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        if (bytea == null || bytea.isEmpty()) {
            this.caches[uuid] = StatisticsEntity()
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
                Statistic.entries.forEach { entry -> setStatistic(entry, 0) }
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
            val entries = Statistic.entries
            buf.writeInt(entries.size)
            entries.forEach { k ->
                val bytes = k.name.toByteArray(Charsets.UTF_8)
                buf.writeInt(bytes.size)
                buf.writeBytes(bytes)
                buf.writeInt(player.getStatistic(k))
            }
            buf.array()
        }
    }

    override fun wrapper(bytea: ByteArray): StatisticsEntity {
        val result = StatisticsEntity()
        val buf = Unpooled.wrappedBuffer(bytea)
        val size = buf.readInt()
        for (i in 0 until size) {
            val length = buf.readInt()
            val bytes = ByteArray(length)
            buf.readBytes(bytes)
            val str = String(bytes, Charsets.UTF_8)
            val statistic = Statistic.valueOf(str)
            val value = buf.readInt()
            result.statistics[statistic] = value
        }
        return result
    }
}