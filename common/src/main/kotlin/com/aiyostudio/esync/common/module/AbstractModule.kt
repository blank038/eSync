package com.aiyostudio.esync.common.module

import java.util.UUID

abstract class AbstractModule<T : IEntity> : IModule<T> {
    protected val caches = mutableMapOf<UUID, T>()

    override fun init() {}

    override fun preLoad(uuid: UUID) {}

    override fun find(uuid: UUID): T? {
        return caches[uuid]
    }

    override fun unloadCache(uuid: UUID) {
        this.caches.remove(uuid)
    }

    override fun unload() {
        caches.clear()
    }
}