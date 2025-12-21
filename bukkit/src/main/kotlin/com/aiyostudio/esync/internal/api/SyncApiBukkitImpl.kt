package com.aiyostudio.esync.internal.api

import com.aiyostudio.esync.api.SyncApi
import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.common.module.IModule
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import java.util.UUID

class SyncApiBukkitImpl : SyncApi {

    override fun registerModule(uniqueKey: String, module: IModule<IEntity>): Boolean {
        TODO("Not yet implemented")
    }

    override fun updateState(
        uuid: UUID,
        moduleId: String,
        state: SyncState
    ): Boolean {
        return RepositoryHandler.repository?.updateState(uuid, moduleId, state) ?: false
    }
}