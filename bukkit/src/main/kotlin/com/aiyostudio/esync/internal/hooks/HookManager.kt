package com.aiyostudio.esync.internal.hooks

import org.bukkit.Bukkit

object HookManager {
    private const val HOOK_PACKAGE = "com.aiyostudio.esync.internal.hooks"
    private val hooks = mutableMapOf(
        "Chemdah" to arrayOf("$HOOK_PACKAGE.chemdah.ChemdahHook")
    )

    fun init() {
        hooks.filter { Bukkit.getPluginManager().getPlugin(it.key) != null }
            .forEach { (_, v) ->
                v.forEach {
                    try {
                        Class.forName(it).newInstance()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
    }
}