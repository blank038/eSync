package com.aiyostudio.esync.common.module

import java.util.UUID

interface IModule<T> {
    val uniqueKey: String

    fun attemptLoad(uuid: UUID, bytea: ByteArray)

    fun load(uuid: UUID, bytea: ByteArray): T

    fun toByteArray(uuid: UUID): ByteArray
}