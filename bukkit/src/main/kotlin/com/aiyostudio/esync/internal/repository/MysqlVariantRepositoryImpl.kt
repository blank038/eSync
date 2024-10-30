package com.aiyostudio.esync.internal.repository

import com.aiyostudio.esync.common.repository.impl.MysqlRepositoryImpl
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aystudio.core.bukkit.util.mysql.MySqlStorageHandler
import java.sql.Connection

class MysqlVariantRepositoryImpl(
    url: String,
    user: String,
    password: String
) : MysqlRepositoryImpl(url, user, password) {
    override val id: String = "MySQL-Variant"
    private val source = MySqlStorageHandler(
        EfficientSyncBukkit.instance,
        url,
        user,
        password,
        *sql
    )

    override fun init() {
        source.setCheckConnection(true)
        source.reconnectionQueryTable = esyncDataTable
    }

    override fun getConnection(): Connection {
        return source.dataSource.connection
    }
}