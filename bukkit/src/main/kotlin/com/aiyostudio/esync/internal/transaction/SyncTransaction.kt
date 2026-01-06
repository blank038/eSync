package com.aiyostudio.esync.internal.transaction

import com.aiyostudio.esync.common.EfficientSync
import com.aiyostudio.esync.common.enums.SyncState
import com.aiyostudio.esync.internal.api.event.PlayerModuleEvent
import com.aiyostudio.esync.internal.config.SyncConfig
import com.aiyostudio.esync.internal.enums.TransactionState
import com.aiyostudio.esync.internal.handler.CacheHandler
import com.aiyostudio.esync.internal.handler.ModuleHandler
import com.aiyostudio.esync.internal.handler.RepositoryHandler
import com.aiyostudio.esync.internal.plugin.EfficientSyncBukkit
import com.aiyostudio.esync.internal.util.PlayerUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Level
import kotlin.Exception

class SyncTransaction(
    private val uuid: UUID,
    private val modules: List<String>,
    private val success: (uuid: UUID) -> Unit = {},
    private val failed: (uuid: UUID) -> Unit = {}
) {
    private val completeList = mutableSetOf<String>()
    private var cancelled = false

    fun start() {
        val list = modules.map { k ->
            CompletableFuture.supplyAsync {
                if (PlayerUtil.isOnline(uuid)) {
                    val task = SyncTask(uuid, k).execute()
                    val state = task.getState()
                    if (state == TransactionState.COMPLETE) {
                        completeList.add(k)
                    } else {
                        val stage = task.getStage()
                        val reason = task.getFailureReason() ?: "unknown"
                        EfficientSyncBukkit.instance.logger.warning(
                            "Failed to sync module '$k' for player $uuid | stage: ${stage.description} | reason: $reason"
                        )
                    }
                    state
                } else {
                    EfficientSyncBukkit.instance.logger.warning("Player $uuid is offline, skipping sync module '$k'")
                    TransactionState.FAILED
                }
            }.exceptionally {
                EfficientSyncBukkit.instance.logger.log(Level.WARNING, it) { "Failed to sync $k data" }
                TransactionState.FAILED
            }
        }.toList()
        CompletableFuture.allOf(*list.toTypedArray())
            .thenApply { list.stream().allMatch { v -> v.isDone && v.get() == TransactionState.COMPLETE } }
            .thenAccept { allCompleted ->
                Bukkit.getPlayer(uuid)?.takeIf { it.isOnline }?.let {
                    val hasCache = CacheHandler.playerCaches.containsKey(uuid)
                    val result = hasCache && allCompleted && !cancelled
                    if (result) {
                        syncApply(it)
                    } else {
                        if (!hasCache) {
                            EfficientSyncBukkit.instance.logger.warning("Player cache not found for $uuid")
                        }
                        if (cancelled) {
                            EfficientSyncBukkit.instance.logger.warning("Sync transaction cancelled for $uuid")
                        }
                        failed(uuid)
                    }
                } ?: run {
                    EfficientSyncBukkit.instance.logger.warning("Player $uuid is offline when applying data")
                    failed()
                }
            }
    }

    private fun failed() {
        val repository = RepositoryHandler.repository ?: return
        this.completeList.forEach { v -> repository.updateState(uuid, v, SyncState.COMPLETE) }
        failed(uuid)
    }

    private fun syncApply(player: Player) {
        Bukkit.getScheduler().runTask(EfficientSyncBukkit.instance) { apply(player) }
    }

    private fun apply(player: Player) {
        val playerCache = CacheHandler.playerCaches[player.uniqueId] ?: run {
            EfficientSyncBukkit.instance.logger.warning("Player cache not found when applying data for ${player.name}")
            failed(uuid)
            return
        }

        try {
            // 批量处理所有模块，减少线程切换开销
            val successfulModules = mutableListOf<String>()

            for (moduleId in modules) {
                try {
                    // 异步更新玩家模块状态
                    Bukkit.getScheduler().runTaskAsynchronously(EfficientSyncBukkit.instance) {
                        EfficientSync.api.updateState(uuid, moduleId, SyncState.LOCKED)
                    }
                    // 获取模块数据并应用模块数据
                    val module = ModuleHandler.findByKey(moduleId)
                    if (module != null && module.apply(player.uniqueId)) {
                        // 玩家缓存加载
                        playerCache.load(module.uniqueKey)
                        successfulModules.add(module.uniqueKey)
                    } else {
                        EfficientSyncBukkit.instance.logger.warning(
                            "Failed to apply module $moduleId for player ${player.name}"
                        )
                        failed(uuid)
                        return
                    }
                } catch (e: Exception) {
                    EfficientSyncBukkit.instance.logger.log(
                        Level.WARNING,
                        e
                    ) { "Failed to process module $moduleId for ${player.name}" }
                    failed(uuid)
                    return
                }
            }

            // 批量执行所有模块加载事件
            successfulModules.forEach { uniqueKey ->
                try {
                    val event = PlayerModuleEvent.Loaded(player, uniqueKey)
                    Bukkit.getPluginManager().callEvent(event)
                } catch (e: Exception) {
                    EfficientSyncBukkit.instance.logger.log(
                        Level.WARNING,
                        e
                    ) { "Failed to execute loaded event for module $uniqueKey" }
                    // 事件失败不影响其他事件处理
                }
            }

            // 如果需要，添加依赖加载事件
            try {
                if (playerCache.checkDepends()) {
                    val dependEvent = PlayerModuleEvent.DependLoaded(player)
                    Bukkit.getPluginManager().callEvent(dependEvent)
                }
            } catch (e: Exception) {
                EfficientSyncBukkit.instance.logger.log(
                    Level.WARNING,
                    e
                ) { "Failed to execute depend loaded event for ${player.name}" }
                // 事件失败不影响整体处理
            }

            // 最终执行成功回调
            success(uuid)
        } catch (e: Exception) {
            EfficientSyncBukkit.instance.logger.log(
                Level.WARNING,
                e
            ) { "Unexpected error while applying data for ${player.name}" }
            failed(uuid)
        }
    }
}

private class SyncTask(
    private val uuid: UUID,
    private val moduleId: String
) {
    private val logger = EfficientSyncBukkit.instance.logger
    private val debug = SyncConfig.debug
    private val module = ModuleHandler.findByKey(moduleId)
    private val repository = RepositoryHandler.repository
    private var stage = SyncTaskStage.INIT
    private var state: TransactionState = TransactionState.FAILED
    private var failureReason: String? = null

    private fun debug(message: String) {
        if (!debug) {
            return
        }
        if (!SyncConfig.debugModules.isEmpty() && !SyncConfig.debugModules.contains(moduleId)) {
            return
        }
        if (!SyncConfig.debugUUIDs.isEmpty() && !SyncConfig.debugUUIDs.contains(uuid.toString())) {
            return
        }
        logger.info("[eSync-Debug] [$moduleId] $message")
    }

    private fun preload(): Boolean {
        stage = SyncTaskStage.PRELOAD_CHECK_EXISTS
        try {
            debug("Checking if data exists for $uuid")
            val exists = repository!!.isExists(uuid, module!!.uniqueKey)
            debug("Data exists: $exists")
            
            if (exists) {
                stage = SyncTaskStage.PRELOAD_LOAD_EXISTING
                debug("Loading existing data via preLoad()")
                module.preLoad(uuid)
                debug("preLoad() completed successfully")
                return true
            }
            
            stage = SyncTaskStage.PRELOAD_FIRST_LOAD
            debug("No existing data, performing firstLoad()")
            val bytea = module.toByteArray(uuid)
            debug("Generated initial data, size: ${bytea?.size ?: 0} bytes")
            
            if (module.firstLoad(uuid, bytea)) {
                debug("firstLoad() succeeded")
                state = TransactionState.COMPLETE
                return true
            }
            failureReason = "firstLoad returned false (data initialization failed)"
            debug("firstLoad() returned false")
            return false
        } catch (e: Exception) {
            failureReason = "preload exception: ${e.message}"
            logger.log(Level.WARNING, e) { "[SyncTask] Module '$moduleId' preload failed for $uuid" }
            return false
        }
    }

    private fun attemptLoad(): Boolean {
        if (state == TransactionState.COMPLETE) return true
        stage = SyncTaskStage.ATTEMPT_LOAD_WAIT_STATE
        try {
            val autoUnlock = SyncConfig.autoUnlock?.getBoolean("enable", true) ?: true
            val tick = 1.coerceAtLeast(if (autoUnlock) SyncConfig.autoUnlock?.getInt("delay", 20) ?: 20 else 1)
            debug("Starting state wait loop (autoUnlock: $autoUnlock, maxWait: ${tick}s)")
            
            var syncState: SyncState? = null
            for (i in 0 until tick) {
                if (!PlayerUtil.isOnline(uuid)) {
                    failureReason = "player went offline during attemptLoad (tick $i/$tick)"
                    debug(failureReason!!)
                    break
                }
                syncState = repository!!.queryState(uuid, module!!.uniqueKey)
                debug("[${i + 1}/$tick] queryState() returned: $syncState")
                
                if (syncState == SyncState.COMPLETE) {
                    debug("State is COMPLETE, proceeding to load data")
                    break
                }
                if (!autoUnlock) {
                    debug("autoUnlock disabled, not waiting further")
                    break
                }
                if (i + 1 == tick) {
                    stage = SyncTaskStage.ATTEMPT_LOAD_FORCE_UNLOCK
                    debug("Max wait reached, attempting force unlock...")
                    val unlockResult = repository.updateState(uuid, module.uniqueKey, SyncState.COMPLETE)
                    debug("Force unlock result: $unlockResult")
                    if (unlockResult) {
                        syncState = SyncState.COMPLETE
                        break
                    }
                }
                Thread.sleep(1000L)
            }
            
            if (syncState == SyncState.COMPLETE) {
                stage = SyncTaskStage.ATTEMPT_LOAD_QUERY_DATA
                debug("Querying data from repository...")
                val bytea = repository!!.queryData(uuid, module!!.uniqueKey)
                debug("Data retrieved, size: ${bytea?.size ?: 0} bytes")
                
                stage = SyncTaskStage.ATTEMPT_LOAD_DESERIALIZE
                debug("Calling module.attemptLoad() to deserialize...")
                if (!module.attemptLoad(uuid, bytea)) {
                    failureReason = "attemptLoad returned false (data deserialization or loading failed, dataSize: ${bytea?.size ?: 0})"
                    debug(failureReason!!)
                    return false
                }
                debug("module.attemptLoad() succeeded")
                
                // 检测模块 isReady 状态，最多轮询5次，每次间隔1秒
                stage = SyncTaskStage.ATTEMPT_LOAD_CHECK_READY
                debug("Checking module isReady() status...")
                var isReadyResult = false
                for (readyCheck in 1..5) {
                    if (!PlayerUtil.isOnline(uuid)) {
                        failureReason = "player went offline during isReady check (attempt $readyCheck/5)"
                        debug(failureReason!!)
                        return false
                    }
                    isReadyResult = module.isReady(uuid)
                    debug("[$readyCheck/5] isReady() returned: $isReadyResult")
                    if (isReadyResult) {
                        debug("Module is ready")
                        break
                    }
                    if (readyCheck < 5) {
                        Thread.sleep(1000L)
                    }
                }
                if (!isReadyResult) {
                    failureReason = "module isReady() returned false after 5 attempts"
                    debug(failureReason!!)
                    return false
                }
                
                stage = SyncTaskStage.ATTEMPT_LOAD_UPDATE_STATE
                debug("Updating state to WAITING...")
                if (!repository.updateState(uuid, module.uniqueKey, SyncState.WAITING)) {
                    failureReason = "failed to update state to WAITING after loading"
                    debug(failureReason!!)
                    return false
                }
                debug("State updated to WAITING successfully")
                state = TransactionState.COMPLETE
                return true
            }
            failureReason = "syncState not COMPLETE after waiting (state: $syncState, autoUnlock: $autoUnlock, waited: ${tick}s)"
            debug(failureReason!!)
            return false
        } catch (e: Exception) {
            failureReason = "attemptLoad exception: ${e.message}"
            logger.log(Level.WARNING, e) { "[SyncTask] Module '$moduleId' attemptLoad failed for $uuid" }
            return false
        }
    }

    fun execute(): SyncTask {
        debug("Starting sync task for $uuid")
        if (module == null) {
            failureReason = "module '$moduleId' not found"
            debug(failureReason!!)
            this.state = TransactionState.FAILED
            return this
        }
        if (repository == null) {
            failureReason = "repository not initialized"
            debug(failureReason!!)
            this.state = TransactionState.FAILED
            return this
        }
        if (this.stage == SyncTaskStage.INIT && this.preload()) {
            stage = SyncTaskStage.PRELOAD_COMPLETE
            debug("Preload stage completed")
        }
        if (this.stage == SyncTaskStage.PRELOAD_COMPLETE) {
            this.attemptLoad()
        }
        debug("Sync task finished, state: $state, stage: $stage")
        return this
    }

    fun getState(): TransactionState {
        return this.state
    }

    fun getFailureReason(): String? {
        return failureReason
    }

    fun getStage(): SyncTaskStage {
        return stage
    }
}

private enum class SyncTaskStage(val description: String) {
    INIT("initializing"),
    PRELOAD_CHECK_EXISTS("checking if data exists"),
    PRELOAD_LOAD_EXISTING("loading existing data"),
    PRELOAD_FIRST_LOAD("performing first load"),
    PRELOAD_COMPLETE("preload completed"),
    ATTEMPT_LOAD_WAIT_STATE("waiting for state to be COMPLETE"),
    ATTEMPT_LOAD_FORCE_UNLOCK("force unlocking"),
    ATTEMPT_LOAD_QUERY_DATA("querying data from repository"),
    ATTEMPT_LOAD_DESERIALIZE("deserializing data"),
    ATTEMPT_LOAD_CHECK_READY("checking module isReady status"),
    ATTEMPT_LOAD_UPDATE_STATE("updating state to WAITING")
}