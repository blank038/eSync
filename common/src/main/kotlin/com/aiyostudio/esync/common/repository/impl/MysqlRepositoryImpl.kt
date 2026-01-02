package com.aiyostudio.esync.common.repository.impl

import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.common.repository.IRepository
import java.io.ByteArrayInputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

open class MysqlRepositoryImpl(
    private val url: String,
    private val user: String,
    private val password: String
) : IRepository {
    protected val esyncDataTable = "esync_data"
    protected open val sql = arrayOf(
        """
            CREATE TABLE IF NOT EXISTS $esyncDataTable
            (
                `id`         BIGINT AUTO_INCREMENT NOT NULL,
                `owner_uuid` VARCHAR(40)           NOT NULL,
                `module`     VARCHAR(100)          NOT NULL,
                `data`       MEDIUMBLOB            NOT NULL,
                `state`      ENUM ('COMPLETE', 'WAITING', 'LOCKED'),
                PRIMARY KEY (`id`),
                INDEX idx_owner (owner_uuid),
                INDEX idx_state (state),
                INDEX idx_module (module)
            ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        """.trimIndent()
    )
    override val id = "MySQL"

    override fun init() {
        sql.forEach { line ->
            this.connect {
                it.use { conn -> conn.prepareStatement(line).executeUpdate() }
            }
        }
    }

    override fun isExists(uuid: UUID, module: String): Boolean {
        val result = AtomicBoolean(false)
        this.connect {
            val sql = "SELECT state FROM $esyncDataTable WHERE owner_uuid = ? AND module = ?"
            it.use { conn ->
                conn.prepareStatement(sql).use { statement ->
                    statement.setString(1, uuid.toString())
                    statement.setString(2, module)
                    statement.executeQuery().use { rs -> result.set(rs.next()) }
                }
            }
        }
        return result.get()
    }

    override fun queryData(uuid: UUID, module: String): ByteArray? {
        val result = AtomicReference<ByteArray>()
        this.connect { conn ->
            val sql = "SELECT data FROM $esyncDataTable WHERE owner_uuid = ? AND module = ?"
            conn.prepareStatement(sql).use { statement ->
                statement.setString(1, uuid.toString())
                statement.setString(2, module)
                statement.executeQuery().use rsUse@{ rs ->
                    if (!rs.next()) {
                        return@rsUse
                    }
                    rs.getBlob(1).binaryStream.use {
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
        val txSuccess = this.connectTransaction(uuid, module) { conn, id ->
            val sql = "UPDATE $esyncDataTable SET data = ? WHERE id = ?"
            conn.prepareStatement(sql).use { statement ->
                statement.setBlob(1, ByteArrayInputStream(bytea))
                statement.setInt(2, id)
                result.set(statement.executeUpdate() > 0)
            }
        }
        return txSuccess && result.get()
    }

    override fun updateState(uuid: UUID, module: String, state: SyncState): Boolean {
        if (!this.isExists(uuid, module)) return false
        val result = AtomicBoolean(false)
        val txSuccess = this.connectTransaction(uuid, module) { conn, id ->
            val sql = "UPDATE $esyncDataTable SET state = ? WHERE id = ?"
            conn.prepareStatement(sql).use { statement ->
                statement.setString(1, state.name)
                statement.setInt(2, id)
                result.set(statement.executeUpdate() > 0)
            }
        }
        return txSuccess && result.get()
    }

    override fun disable() {
    }

    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 100L
        private const val MYSQL_DEADLOCK_ERROR_CODE = 1213
        private const val POSTGRES_DEADLOCK_SQLSTATE = "40P01"
    }

    open fun connectTransaction(uuid: UUID, module: String, block: (connect: Connection, id: Int) -> Unit): Boolean {
        var retryCount = 0
        while (retryCount < MAX_RETRY_COUNT) {
            val result = executeTransaction(uuid, module, block)
            if (result.first) {
                return true
            }
            // 检查是否是死锁错误
            if (result.second) {
                retryCount++
                if (retryCount < MAX_RETRY_COUNT) {
                    Thread.sleep(RETRY_DELAY_MS * retryCount)
                }
            } else {
                // 非死锁错误，直接返回失败
                return false
            }
        }
        return false
    }

    private fun executeTransaction(
        uuid: UUID,
        module: String,
        block: (connect: Connection, id: Int) -> Unit
    ): Pair<Boolean, Boolean> {
        var success = false
        var isDeadlock = false
        this.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
                val lockQuery = "SELECT id FROM esync_data WHERE owner_uuid = ? AND module = ? FOR UPDATE;"
                val id = conn.prepareStatement(lockQuery).use { statement ->
                    statement.setString(1, uuid.toString())
                    statement.setString(2, module)
                    statement.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else -1 }
                }
                if (id != -1) {
                    block(conn, id)
                    conn.commit()
                    success = true
                }
            } catch (e: Exception) {
                if (e is SQLException) {
                    isDeadlock = e.errorCode == MYSQL_DEADLOCK_ERROR_CODE ||
                            e.sqlState == POSTGRES_DEADLOCK_SQLSTATE ||
                            e.message?.contains("Deadlock", ignoreCase = true) == true
                }
                if (!isDeadlock) e.printStackTrace()
            } finally {
                if (!success) runCatching { conn.rollback() }.onFailure { it.printStackTrace() }
                conn.autoCommit = true
            }
        }
        return Pair(success, isDeadlock)
    }

    open fun connect(block: (connect: Connection) -> Unit) {
        with(this.getConnection()) { this.use { block(this) } }
    }

    open fun getConnection(): Connection {
        return DriverManager.getConnection(url, user, password)
    }
}