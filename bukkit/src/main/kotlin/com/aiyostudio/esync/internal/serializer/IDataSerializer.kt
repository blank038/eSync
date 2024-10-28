package com.aiyostudio.esync.internal.serializer

import org.bukkit.inventory.ItemStack

interface IDataSerializer {

    fun serializerItem(itemStack: ItemStack): ByteArray

    fun deserializerItem(byteArray: ByteArray): ItemStack
}