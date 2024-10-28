package com.aiyostudio.esync.internal.api.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class InitModulesEvent : Event() {
    private val handlerList = HandlerList()

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    fun getHandlerList(): HandlerList {
        return handlerList
    }
}