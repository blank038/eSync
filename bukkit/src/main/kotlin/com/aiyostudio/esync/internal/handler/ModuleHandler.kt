package com.aiyostudio.esync.internal.handler

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.common.module.IModule
import com.aiyostudio.esync.internal.api.event.ModuleRegistryEvent
import com.aiyostudio.esync.internal.module.impl.InventoryModuleImpl
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection

object ModuleHandler {
    private val modules = mutableMapOf<String, IModule<IEntity>>()

    fun createDefaultModule(key: String, option: ConfigurationSection): IModule<IEntity>? {
        if (!option.getBoolean("enable")) return null
        when(key) {
            "inventory" -> return InventoryModuleImpl(option)
        }
        return null
    }

    fun register(moduleImpl: IModule<IEntity>): Boolean {
        if (modules.containsKey(moduleImpl.uniqueKey)) return false
        val event = ModuleRegistryEvent(moduleImpl)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return false
        moduleImpl.run {
            modules[this.uniqueKey] = this
            this.init()
        }
        return true
    }

    fun findByKey(key: String): IModule<IEntity>? = modules[key]

    fun unloadAllModule() {
        modules.forEach { (_, v) -> v.unload()}
        modules.clear()
    }
}