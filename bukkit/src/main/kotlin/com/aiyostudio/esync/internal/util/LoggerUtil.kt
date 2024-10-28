package com.aiyostudio.esync.internal.util

import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit

object LoggerUtil {

    fun printHeader() {
        val plugin = EfficientSyncBukkit.instance
        print(" ")
        print("   &3${plugin.name} &bv${plugin.description.version}");
        print(" ")
    }

    fun printFooter() {
        print(" ")
    }

    fun print(text: String, prefix: Boolean = false) {
        EfficientSyncBukkit.instance.consoleLogger.log(prefix, text)
    }
}