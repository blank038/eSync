package com.aiyostudio.esync.internal.api

import com.aiyostudio.esync.api.SyncApi
import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.common.module.IModule

class SyncApiBukkitImpl : SyncApi {

    override fun registerModule(uniqueKey: String, module: IModule<IEntity>): Boolean {
        TODO("Not yet implemented")
    }
}