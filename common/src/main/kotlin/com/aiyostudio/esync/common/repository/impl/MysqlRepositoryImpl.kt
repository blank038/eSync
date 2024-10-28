package com.aiyostudio.esync.common.repository.impl

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.common.repository.IRepository
import java.util.*

class MysqlRepositoryImpl(
    private val url: String,
    private val user: String,
    private val password: String
) : IRepository {

    override fun isExists(uuid: UUID, module: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun queryData(uuid: UUID, module: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun queryState(uuid: UUID, module: String): SyncState? {
        TODO("Not yet implemented")
    }

    override fun insert(uuid: UUID, module: String, bytea: ByteArray, state: SyncState): Boolean {
        TODO("Not yet implemented")
    }

    override fun updateData(uuid: UUID, module: String, bytea: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override fun updateState(uuid: UUID, module: String, state: SyncState): Boolean {
        TODO("Not yet implemented")
    }

    override fun disable() {
        TODO("Not yet implemented")
    }
}