package com.aiyostudio.esync.internal.serializer

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack

abstract class GeneralDataSerializerImpl : IDataSerializer {

    override fun serializerItem(itemStack: ItemStack): ByteArray {
        val yaml = YamlConfiguration()
        yaml.set("item", itemStack)
        return yaml.saveToString().toByteArray(Charsets.UTF_8)
    }

    override fun deserializerItem(byteArray: ByteArray): ItemStack {
        val yamlStr = String(byteArray, Charsets.UTF_8)
        val yaml = YamlConfiguration()
        yaml.loadFromString(yamlStr)
        return yaml.getItemStack("item") ?: ItemStack(Material.AIR)
    }
}