package com.aiyostudio.esync.internal.util

import com.aiyostudio.esync.common.EfficientSync
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.serializer.IDataSerializer
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack


object SerializerUtil {
    private lateinit var serializer: IDataSerializer

    fun init() {
        try {
            val version = Bukkit.getServer().javaClass.getPackage().toString()
                .replace(".", ",")
                .split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[3]
            val classz = Class.forName("${EfficientSync.path}.internal.serializer.$version.DataSerializer")
            serializer = classz.newInstance() as IDataSerializer
            LoggerUtil.print("&6 * &fLoaded version: &e${version}")
        } catch (ex: Exception) {
            EfficientSyncBukkit.instance.logger.severe("The current server version is not supported.")
        }
    }

    fun serializerItem(itemStack: ItemStack): ByteArray {
        return this.serializer.serializerItem(itemStack)
    }

    fun deserializerItem(byteArray: ByteArray): ItemStack {
        return this.serializer.deserializerItem(byteArray)
    }
}