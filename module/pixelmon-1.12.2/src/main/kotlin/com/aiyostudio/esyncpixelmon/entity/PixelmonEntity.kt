package com.aiyostudio.esyncpixelmon.entity

import com.aiyostudio.esync.common.module.IEntity
import com.pixelmonmod.pixelmon.Pixelmon
import net.minecraft.nbt.NBTTagCompound
import java.util.UUID

class PixelmonEntity : IEntity {
    lateinit var partyCompound: NBTTagCompound
    lateinit var pcCompound: NBTTagCompound

    override fun apply(uuid: Any): Boolean {
        if (uuid !is UUID) {
            return false
        }
        val partyStorage = Pixelmon.storageManager.getParty(uuid)
        val pcStorage = Pixelmon.storageManager.getPCForPlayer(uuid)
        partyStorage.readFromNBT(partyCompound)
        pcStorage.readFromNBT(pcCompound)
        return true
    }
}