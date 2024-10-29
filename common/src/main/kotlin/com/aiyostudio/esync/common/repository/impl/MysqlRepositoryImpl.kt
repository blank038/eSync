package com.aiyostudio.esync.common.repository.impl

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.common.repository.IRepository
import java.io.ByteArrayInputStream
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class MysqlRepositoryImpl(
    private val url: String,
    private val user: String,
    private val password: String
) : IRepository {
    private val esyncDataTable = "`esync_data`"
    override val id = "MySQL"

    override fun isExists(uuid: UUID, module: String): Boolean {
        val result = AtomicBoolean(false)
        this.connect {
            val sql = "SELECT state FROM $esyncDataTable WHERE owner_uuid = ? AND module = ?"
            it.prepareStatement(sql).use { statement ->
                statement.setString(1, uuid.toString())
                statement.setString(2, module)
                statement.executeQuery().use { rs -> result.set(rs.next()) }
            }
        }
        return result.get()
    }

    override fun queryData(uuid: UUID, module: String): ByteArray? {
        val result = AtomicReference<ByteArray>()
        this.connect{ conn ->
            val sql = "SELECT data FROM $esyncDataTable WHERE owner_uuid = ? AND module = ?"
            conn.prepareStatement(sql).use { statement ->
                statement.setString(1, uuid.toString())
                statement.setString(2, module)
                statement.executeQuery().use rsUse@ { rs ->
                    if (!rs.next()) {
                        return@rsUse
                    }
                    rs.getBlob(1).getBinaryStream().use {
                        val bytes = ByteArray(it.available())
                        it.read(bytes)
                        result.set(bytes)
                    }
                }
            }
        }
        return result.get()
    }

    override fun queryState(uuid: UUID, module: String): SyncState? {
        val result = AtomicReference<SyncState>()
        this.connect {
            val sql = "SELECT state FROM $esyncDataTable WHERE owner_uuid = ? AND module = ?"
            it.prepareStatement(sql).use { statement ->
                statement.setString(1, uuid.toString())
                statement.setString(2, module)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        result.set(SyncState.valueOf(rs.getString(1)))
                    }
                }
            }
        }
        return result.get()
    }

    override fun insert(uuid: UUID, module: String, bytea: ByteArray, state: SyncState): Boolean {
        if (this.isExists(uuid, module)) {
            return this.updateData(uuid, module, bytea) && this.updateState(uuid, module, state)
        }
        val result = AtomicBoolean(false)
        this.connect {
            val sql = "INSERT INTO $esyncDataTable(owner_uuid, module, data, state) VALUES (?, ?, ?, ?)"
            it.prepareStatement(sql).use { statement ->
                statement.setString(1, uuid.toString())
                statement.setString(2, module)
                statement.setBlob(3, ByteArrayInputStream(bytea))
                statement.setString(4, state.name)
                result.set(statement.executeUpdate() > 0)
            }
        }
        return result.get()
    }

    override fun updateData(uuid: UUID, module: String, bytea: ByteArray): Boolean {
        if (!this.isExists(uuid, module)) return false
        val result = AtomicBoolean(false)
        this.connectTransaction(uuid, module) { conn, id ->
            val sql = "UPDATE $esyncDataTable SET data = ? WHERE id = ?"
            conn.prepareStatement(sql).use { statement ->
                statement.setBlob(1, ByteArrayInputStream(bytea))
                statement.setInt(2, id)
                result.set(statement.executeUpdate() > 0)
            }
        }
        return result.get()
    }

    override fun updateState(uuid: UUID, module: String, state: SyncState): Boolean {
        if (!this.isExists(uuid, module)) return false
        val result = AtomicBoolean(false)
        this.connectTransaction(uuid, module) { conn, id ->
            val sql = "UPDATE $esyncDataTable SET state = ? WHERE id = ?"
            conn.prepareStatement(sql).use { statement ->
                statement.setString(1, state.name)
                statement.setInt(2, id)
                result.set(statement.executeUpdate() > 0)
            }
        }
        return result.get()
    }

    override fun disable() {
    }

    private fun connectTransaction(uuid: UUID, module: String, block: (connect: Connection, id: Int) -> Unit) {
        with(DriverManager.getConnection(url, user, password)) {
            this.autoCommit = false
            try {
                this.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
                val lockQuery = "SELECT id FROM esync_data WHERE owner_uuid = ? AND module = ? FOR UPDATE;"
                val id = AtomicInteger(-1)
                this.prepareStatement(lockQuery).use { statement ->
                    statement.setString(1, uuid.toString())
                    statement.setString(2, module)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            id.set(rs.getInt(1))
                        }
                    }
                }
                block(this, id.get())
                this.commit()
            } catch (e: Exception) {
                this.rollback()
            } finally {
                this.close()
            }
        }
    }

    private fun connect(block: (connect: Connection) -> Unit) {
        with(DriverManager.getConnection(url, user, password)) { block(this) }
    }
}