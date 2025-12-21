package com.aiyostudio.esync.api

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.common.module.IModule
import java.util.UUID

interface SyncApi {

    fun registerModule(uniqueKey: String, module: IModule<IEntity>): Boolean

    fun updateState(uuid: UUID, moduleId: String, state: SyncState): Boolean
}