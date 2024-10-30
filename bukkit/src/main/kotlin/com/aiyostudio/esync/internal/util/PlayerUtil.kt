package com.aiyostudio.esync.internal.util

import org.bukkit.Bukkit
import java.util.UUID

object PlayerUtil {

    fun isOnline(uuid: UUID): Boolean {
        return Bukkit.getPlayer(uuid)?.isOnline ?: false
    }
}