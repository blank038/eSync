package com.aiyostudio.esync.internal.api.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class InitModulesEvent : Event() {
    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }
}