package com.aiyostudio.esync.common.module

import java.util.UUID

abstract class AbstractModule<T : IEntity> : IModule<T> {
    protected val caches = mutableMapOf<UUID, T>()

    override fun init() {}

    override fun unload() {
        caches.clear()
    }
}