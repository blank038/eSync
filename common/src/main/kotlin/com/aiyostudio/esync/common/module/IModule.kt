package com.aiyostudio.esync.common.module

import java.util.UUID

interface IModule<out T : IEntity> {
    val uniqueKey: String

    fun init()

    fun attemptLoad(uuid: UUID, bytea: ByteArray?): Boolean

    fun find(uuid: UUID): T?

    fun toByteArray(uuid: UUID): ByteArray?

    fun unloadCache(uuid: UUID)

    fun unload()
}