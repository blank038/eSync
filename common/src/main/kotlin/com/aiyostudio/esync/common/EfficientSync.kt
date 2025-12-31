package com.aiyostudio.esync.common

import com.aiyostudio.esync.api.SyncApi

object EfficientSync {
    lateinit var api: SyncApi
    const val PATH = "com.aiyostudio.esync"
    const val SERIALIZER_PATH = "com.aiyostudio.esync.internal.serializer"
}