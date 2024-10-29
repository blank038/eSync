package com.aiyostudio.esync.internal.handler

import com.aiyostudio.esync.internal.cache.PlayerCache
import java.util.UUID

object CacheHandler {
    val playerCaches = mutableMapOf<UUID, PlayerCache>()
    val dependModules = mutableListOf<String>()
}