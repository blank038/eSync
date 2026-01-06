package com.aiyostudio.esync.internal.module.impl

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esync.internal.module.entity.LocationEntity
import io.netty.buffer.Unpooled
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import java.nio.charset.StandardCharsets
import java.util.*

class LocationModuleImpl(
    private val option: ConfigurationSection
) : AbstractModule<LocationEntity>() {
    override val uniqueKey: String = "location"

    override fun firstLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        if (bytea == null || bytea.isEmpty()) {
            this.caches[uuid] = LocationEntity()
        } else {
            this.caches[uuid] = wrapper(bytea)
        }
        return true
    }

    override fun attemptLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        return firstLoad(uuid, bytea)
    }

    override fun preLoad(uuid: UUID) {
        // location 模块不需要 preLoad 操作
    }

    override fun apply(uuid: UUID): Boolean {
        val player = Bukkit.getPlayer(uuid)
        return this.find(uuid)?.apply(player) ?: false
    }

    override fun toByteArray(uuid: UUID): ByteArray? {
        val player = Bukkit.getPlayer(uuid)
        return player?.let {
            val location = it.location
            val buf = Unpooled.buffer()
            try {
                val worldNameBytes = location.world?.name?.toByteArray(StandardCharsets.UTF_8) ?: byteArrayOf()
                buf.writeInt(worldNameBytes.size)
                buf.writeBytes(worldNameBytes)
                buf.writeDouble(location.x)
                buf.writeDouble(location.y)
                buf.writeDouble(location.z)
                buf.writeFloat(location.yaw)
                buf.writeFloat(location.pitch)
                buf.array()
            } finally {
                buf.release()
            }
        }
    }

    override fun wrapper(bytea: ByteArray): LocationEntity {
        val result = LocationEntity()
        val buf = Unpooled.wrappedBuffer(bytea)
        try {
            val worldNameLength = buf.readInt()
            val worldNameBytes = ByteArray(worldNameLength)
            buf.readBytes(worldNameBytes)
            result.worldName = String(worldNameBytes, StandardCharsets.UTF_8)
            result.x = buf.readDouble()
            result.y = buf.readDouble()
            result.z = buf.readDouble()
            result.yaw = buf.readFloat()
            result.pitch = buf.readFloat()
            return result
        } finally {
            buf.release()
        }
    }
}
