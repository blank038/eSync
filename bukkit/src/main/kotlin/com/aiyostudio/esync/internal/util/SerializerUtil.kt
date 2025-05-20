package com.aiyostudio.esync.internal.util

import com.aiyostudio.esync.common.EfficientSync
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.serializer.IDataSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.logging.Level


object SerializerUtil {
    private lateinit var serializer: IDataSerializer

    fun init(): Boolean {
        try {
            val version = Bukkit.getServer().javaClass.getPackage().toString()
                .replace(".", ",")
                .split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[3]
            val classz = Class.forName("${EfficientSync.path}.internal.serializer.$version.DataSerializer")
            serializer = classz.newInstance() as IDataSerializer
            LoggerUtil.print("&6 * &fLoaded version: &e${version}")
            return true
        } catch (ex: Exception) {
            EfficientSyncBukkit.instance.logger.log(Level.SEVERE, ex) { "The current server version is not supported." }
            return false
        }
    }

    fun serializerItem(itemStack: ItemStack): ByteArray {
        return this.serializer.serializerItem(itemStack)
    }

    fun deserializerItem(byteArray: ByteArray): ItemStack {
        return this.serializer.deserializerItem(byteArray)
    }

    fun serializerStatistics(player: Player): String? {
        return this.serializer.serializerStatistics(player)
    }

    fun deserializerStatistics(player: Player, str: String?): Boolean {
        return this.serializer.deserializerStatistics(player, str)
    }
}