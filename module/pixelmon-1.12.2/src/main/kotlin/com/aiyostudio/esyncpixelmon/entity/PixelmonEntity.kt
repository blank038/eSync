package com.aiyostudio.esyncpixelmon.entity

import com.aiyostudio.esync.common.module.IEntity
import com.pixelmonmod.pixelmon.Pixelmon
import com.pixelmonmod.pixelmon.listener.PixelmonPlayerTracker
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.gameevent.PlayerEvent
import java.util.*


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
        val event = PlayerEvent.PlayerLoggedInEvent(partyStorage.player)
        PixelmonPlayerTracker.onPlayerLogin(event)
        Pixelmon.storageManager.initializePCForPlayer(partyStorage.player, pcStorage)
        return true
    }
}