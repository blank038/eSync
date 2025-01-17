package com.aiyostudio.esyncpixelmon.module

import com.aiyostudio.esync.common.module.AbstractModule
import com.aiyostudio.esyncpixelmon.SyncPixelmon
import com.aiyostudio.esyncpixelmon.entity.PixelmonEntity
import com.pixelmonmod.pixelmon.Pixelmon
import com.pixelmonmod.pixelmon.api.storage.PCBox
import io.netty.buffer.Unpooled
import net.minecraft.nbt.JsonToNBT
import net.minecraft.nbt.NBTTagCompound
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
            val partyStorage = Pixelmon.storageManager.getParty(uuid)
            val pcStorage = Pixelmon.storageManager.getPCForPlayer(uuid)
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
        val partyStorage = Pixelmon.storageManager.getParty(uuid)
        val partyCompound = partyStorage.writeToNBT(NBTTagCompound())
        val partyBytea = partyCompound.toString().toByteArray(Charsets.UTF_8)
        buf.writeInt(partyBytea.size)
        buf.writeBytes(partyBytea)
        // write pc data
        val pcStorage = Pixelmon.storageManager.getPCForPlayer(uuid)
        val pcCompound = pcStorage.writeToNBT(NBTTagCompound())
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
        entity.partyCompound = JsonToNBT.getTagFromJson(String(partyBytea, Charsets.UTF_8))
        // read pc data
        val pcByteaLength = buf.readInt()
        val pcBytea = ByteArray(pcByteaLength)
        buf.readBytes(pcBytea)
        entity.pcCompound = JsonToNBT.getTagFromJson(String(pcBytea, Charsets.UTF_8))
        return entity
    }
}