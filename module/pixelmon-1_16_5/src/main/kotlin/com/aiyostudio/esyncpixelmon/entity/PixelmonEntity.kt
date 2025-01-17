package com.aiyostudio.esyncpixelmon.entity

import com.aiyostudio.esync.common.module.IEntity
import com.pixelmonmod.pixelmon.api.storage.StorageProxy
import com.pixelmonmod.pixelmon.listener.PixelmonPlayerTracker
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.event.entity.player.PlayerEvent
import java.util.*


class PixelmonEntity : IEntity {
    lateinit var partyCompound: CompoundNBT
    lateinit var pcCompound: CompoundNBT

    override fun apply(uuid: Any): Boolean {
        if (uuid !is UUID) {
            return false
        }
        val partyStorage = StorageProxy.getParty(uuid)
        val pcStorage = StorageProxy.getPCForPlayer(uuid)
        partyStorage.readFromNBT(partyCompound)
        pcStorage.readFromNBT(pcCompound)
        val event = PlayerEvent.PlayerLoggedInEvent(partyStorage.player)
        PixelmonPlayerTracker.onPlayerLogin(event)
        StorageProxy.initializePCForPlayer(partyStorage.player, pcStorage)
        return true
    }
}