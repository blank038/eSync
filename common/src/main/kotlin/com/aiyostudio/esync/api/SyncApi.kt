package com.aiyostudio.esync.api

import com.aiyostudio.esync.common.module.IEntity
import com.aiyostudio.esync.common.module.IModule

interface SyncApi {

    fun registerModule(uniqueKey: String, module: IModule<IEntity>): Boolean
}