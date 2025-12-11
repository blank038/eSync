package com.aiyostudio.esync.internal.api.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

abstract class PlayerModuleEvent(player: Player) : PlayerEvent(player) {

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

    class Loaded(player: Player, val module: String) : PlayerModuleEvent(player)

    class DependLoaded(player: Player) : PlayerModuleEvent(player)
}