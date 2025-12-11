package com.aiyostudio.esync.internal.cache

import com.aiyostudio.esync.internal.handler.CacheHandler

class PlayerCache {
    private val loadedModules = mutableSetOf<String>()
    var dependLoaded = false

    fun load(module: String) {
        this.loadedModules.add(module)
    }

    fun checkDepends(): Boolean {
        if (!dependLoaded && CacheHandler.dependModules.all { it in loadedModules }) {
            dependLoaded = true
            return true
        }
        return false
    }

    fun getLoadedModules(): Set<String> = loadedModules
}