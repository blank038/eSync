package com.aiyostudio.esync.internal.plugin

import com.aystudio.core.bukkit.plugin.AyPlugin

class EfficientSync : AyPlugin() {

    companion object {
        lateinit var instance: AyPlugin
    }

    override fun onEnable() {
        instance = this

    }
}