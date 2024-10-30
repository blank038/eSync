package com.aiyostudio.esync.common.repository

import com.aiyostudio.esync.common.enums.SyncState
import java.util.UUID

interface IRepository {
    val id: String

    fun init()

    fun isExists(uuid: UUID, module: String): Boolean

    fun queryData(uuid: UUID, module: String): ByteArray?

    fun queryState(uuid: UUID, module: String): SyncState?

    fun insert(uuid: UUID, module: String, bytea: ByteArray, state: SyncState): Boolean

    fun updateData(uuid: UUID, module: String, bytea: ByteArray): Boolean

    fun updateState(uuid: UUID, module: String, state: SyncState): Boolean

    fun disable()
}