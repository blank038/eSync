package com.aiyostudio.esync.common.module

import java.util.UUID

interface IModule<out T : IEntity> {
    val uniqueKey: String

    fun init()

    /**
     * Execute before attemptLoad.
     */
    fun preLoad(uuid: UUID)

    /**
     * Called when no player data is present.
     */
    fun firstLoad(uuid: UUID, bytea: ByteArray?): Boolean

    /**
     * Preload data and load into cache.
     */
    fun attemptLoad(uuid: UUID, bytea: ByteArray?): Boolean

    /**
     * Apply after data loading is complete.
     */
    fun apply(uuid: UUID)

    fun find(uuid: UUID): T?

    fun toByteArray(uuid: UUID): ByteArray?

    fun wrapper(bytea: ByteArray): T

    /**
     * When player cache data is unloaded.
     */
    fun unloadCache(uuid: UUID)

    /**
     * Module unloading.
     */
    fun unload()
}