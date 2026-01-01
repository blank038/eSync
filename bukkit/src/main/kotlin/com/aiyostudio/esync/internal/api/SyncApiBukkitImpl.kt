package com.aiyostudio.esync.internal.api

import com.aiyostudio.esync.api.SyncApi
import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.common.module.IModule
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import java.util.UUID

class SyncApiBukkitImpl : SyncApi {

    override fun registerModule(uniqueKey: String, clazz: Class<*>, force: Boolean): Boolean {
        if (!IModule::class.java.isAssignableFrom(clazz)) {
            return false
        }
        if (force || !ModuleHandler.hasModuleFunction(uniqueKey)) {
            ModuleHandler.registerModuleFunction(uniqueKey) { clazz.declaredConstructors.first().newInstance() as IModule<IEntity> }
            return true
        }
        return false
    }

    override fun updateState(
        uuid: UUID,
        moduleId: String,
        state: SyncState
    ): Boolean {
        return RepositoryHandler.repository?.updateState(uuid, moduleId, state) ?: false
    }
}