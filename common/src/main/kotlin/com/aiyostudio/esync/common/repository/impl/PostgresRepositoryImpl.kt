package com.aiyostudio.esync.common.repository.impl

import com.aiyostudio.esync.common.enums.SyncState
import java.sql.Types
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PostgresRepositoryImpl(
    url: String,
    user: String,
    password: String
) : MysqlRepositoryImpl(url, user, password) {
    override val sql = arrayOf(
        """
            DO
            ${'$'}${'$'}
                BEGIN
                    CREATE TYPE sync_state AS ENUM ('COMPLETE', 'WAITING', 'LOCKED');
                EXCEPTION
                    WHEN duplicate_object THEN null;
                END
            ${'$'}${'$'};
        """.trimIndent(),
        """
            CREATE TABLE IF NOT EXISTS $esyncDataTable
            (
                id         SERIAL PRIMARY KEY NOT NULL,
                owner_uuid CHARACTER(40)      NOT NULL,
                module     CHARACTER(100)     NOT NULL,
                data       BYTEA              NOT NULL,
                state      sync_state         NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_owner ON $esyncDataTable (owner_uuid);
            CREATE INDEX IF NOT EXISTS idx_state ON $esyncDataTable (state);
            CREATE INDEX IF NOT EXISTS idx_module ON $esyncDataTable (module);
        """.trimIndent()
    )
    override val id: String = "PostgreSQL"

    init {
        Class.forName("org.postgresql.Driver")
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
                    result.set(rs.getBytes(1))
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
                statement.setBytes(3, bytea)
                statement.setObject(4, state.name, Types.OTHER)
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
                statement.setBytes(1, bytea)
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
                statement.setObject(1, state.name, Types.OTHER)
                statement.setInt(2, id)
                result.set(statement.executeUpdate() > 0)
            }
        }
        return result.get()
    }
}