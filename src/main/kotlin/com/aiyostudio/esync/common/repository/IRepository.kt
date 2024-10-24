package com.aiyostudio.esync.common.repository

import java.util.UUID

interface IRepository {

    /**
     * Attempt to retrieve player data.
     *
     * @param uuid Player Unique Id
     */
    fun attemptLoad(uuid: UUID, uniqueKey: String, serialVersion: Int): Boolean

    /**
     * Load player data.
     */
    fun load(uuid: UUID, uniqueKey: String): Boolean

    /**
     * Save player data.
     */
    fun save(uuid: UUID, uniqueKey: String)

    /**
     * Unload player data.
     */
    fun unload(uuid: UUID)
}