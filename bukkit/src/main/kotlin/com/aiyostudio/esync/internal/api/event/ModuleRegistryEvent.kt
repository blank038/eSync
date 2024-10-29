package com.aiyostudio.esync.internal.api.event

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.common.module.IModule
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ModuleRegistryEvent(
    val module: IModule<IEntity>
) : Event(), Cancellable {
    private val handlerList = HandlerList()
    private var cancelled: Boolean = false

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    fun getHandlerList(): HandlerList {
        return handlerList
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
}