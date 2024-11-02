package com.aiyostudio.esync.internal.serializer

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface IDataSerializer {

    fun serializerItem(itemStack: ItemStack): ByteArray

    fun deserializerItem(byteArray: ByteArray): ItemStack

    fun serializerStatistics(player: Player): String?

    fun deserializerStatistics(player: Player, str: String?): Boolean
}