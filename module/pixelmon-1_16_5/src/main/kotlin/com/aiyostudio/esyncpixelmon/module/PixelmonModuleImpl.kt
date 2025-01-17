package com.aiyostudio.esyncpixelmon.module

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esyncpixelmon.SyncPixelmon
import com.aiyostudio.esyncpixelmon.entity.PixelmonEntity
import com.pixelmonmod.pixelmon.api.storage.PCBox
import com.pixelmonmod.pixelmon.api.storage.StorageProxy
import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.JsonToNBT
import java.util.*

class PixelmonModuleImpl : AbstractModule<PixelmonEntity>() {
    override val uniqueKey: String = "pixelmon"

    override fun firstLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        if (bytea == null || bytea.isEmpty()) {
            this.caches[uuid] = PixelmonEntity()
        } else {
            this.caches[uuid] = wrapper(bytea)
        }
        return true
    }

    override fun attemptLoad(uuid: UUID, bytea: ByteArray?): Boolean {
        return firstLoad(uuid, bytea)
    }

    override fun preLoad(uuid: UUID) {
        if (SyncPixelmon.instance.config.getBoolean("option.always-clear")) {
            val partyStorage = StorageProxy.getParty(uuid)
            val pcStorage = StorageProxy.getPCForPlayer(uuid)
            (0 until 6).forEach { partyStorage.set(it, null) }
            (0 until pcStorage.boxCount).forEach { page ->
                val box = pcStorage.getBox(page)
                (0 until PCBox.POKEMON_PER_BOX).forEach { slot -> box.set(slot, null) }
            }
        }
    }

    override fun apply(uuid: UUID): Boolean {
        return this.find(uuid)?.apply(uuid) ?: false
    }

    override fun toByteArray(uuid: UUID): ByteArray? {
        val buf = Unpooled.buffer()
        // write party data
        val partyStorage = StorageProxy.getParty(uuid)
        val partyCompound = partyStorage.writeToNBT(CompoundNBT())
        val partyBytea = partyCompound.toString().toByteArray(Charsets.UTF_8)
        buf.writeInt(partyBytea.size)
        buf.writeBytes(partyBytea)
        // write pc data
        val pcStorage = StorageProxy.getPCForPlayer(uuid)
        val pcCompound = pcStorage.writeToNBT(CompoundNBT())
        val pcBytea = pcCompound.toString().toByteArray(Charsets.UTF_8)
        buf.writeInt(pcBytea.size)
        buf.writeBytes(pcBytea)
        return buf.array()
    }

    override fun wrapper(bytea: ByteArray): PixelmonEntity {
        val entity = PixelmonEntity()
        val buf = Unpooled.wrappedBuffer(bytea)
        // read party data
        val partyByteaLength = buf.readInt()
        val partyBytea = ByteArray(partyByteaLength)
        buf.readBytes(partyBytea)
        entity.partyCompound = JsonToNBT.parseTag(String(partyBytea, Charsets.UTF_8))
        // read pc data
        val pcByteaLength = buf.readInt()
        val pcBytea = ByteArray(pcByteaLength)
        buf.readBytes(pcBytea)
        entity.pcCompound = JsonToNBT.parseTag(String(pcBytea, Charsets.UTF_8))
        return entity
    }
}