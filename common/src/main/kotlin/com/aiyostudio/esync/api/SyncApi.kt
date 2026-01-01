package com.aiyostudio.esync.api

import com.aiyostudio.esync.common.enums.SyncState
import java.util.UUID

interface SyncApi {

    fun registerModule(uniqueKey: String, clazz: Class<*>, force: Boolean): Boolean

    fun updateState(uuid: UUID, moduleId: String, state: SyncState): Boolean
}