package com.aiyostudio.esync.internal.module.impl

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esync.internal.module.entity.StatisticsEntity
import com.aiyostudio.esync.internal.util.SerializerUtil
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
                Statistic.entries.filter { it.type == Statistic.Type.UNTYPED }
                    .forEach { entry -> setStatistic(entry, 0) }
            }
        }
    }

    override fun apply(uuid: UUID): Boolean {
        val player = Bukkit.getPlayer(uuid)
        return this.find(uuid)?.apply(player) ?: false
    }

    override fun toByteArray(uuid: UUID): ByteArray? {
        val player = Bukkit.getPlayer(uuid)
        val buf = Unpooled.buffer()
        try {
            return player?.let {
                SerializerUtil.serializerStatistics(player)?.let {
                    val bytea = it.toByteArray(Charsets.UTF_8)
                    buf.writeInt(bytea.size)
                    buf.writeBytes(bytea)
                } ?: let {
                    buf.writeInt(0)
                }
                buf.array()
            }
        } finally {
            buf.release()
        }
    }

    override fun wrapper(bytea: ByteArray): StatisticsEntity {
        val result = StatisticsEntity()
        val buf = Unpooled.wrappedBuffer(bytea)
        try {
            val size = buf.readInt()
            val array = ByteArray(size)
            buf.readBytes(array)
            result.data = String(array, Charsets.UTF_8)
            return result
        } finally {
            buf.release()
        }
    }
}