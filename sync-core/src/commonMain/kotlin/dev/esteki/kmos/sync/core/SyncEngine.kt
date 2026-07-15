package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncProgress
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

internal class SyncEngine(
    private val scope: CoroutineScope,
    private val commandChannel: Channel<SyncCommand>,
    private val storageAdapter: StorageAdapter,
    private val transportAdapter: TransportAdapter,
    private val retryPolicy: RetryPolicy,
    private val conflictResolver: ConflictResolver<SyncEntity>,
) {
    private var engineJob: Job? = null
    private var pullCursor: String? = null

    private val _progress = MutableSharedFlow<SyncProgress>(extraBufferCapacity = 64)
    val progress: SharedFlow<SyncProgress> = _progress.asSharedFlow()

    fun start() {
        engineJob = scope.launch {
            for (command in commandChannel) {
                when (command) {
                    is SyncCommand.TriggerSync -> drainPending()
                    is SyncCommand.PullAndSync -> pullAndSync()
                    is SyncCommand.Cancel -> break
                }
            }
        }
    }

    private suspend fun drainPending() {
        val allEntities = storageAdapter.queryAll()
        val pending = allEntities.filter { it.hasPendingOperation }
        val total = pending.size
        var completed = 0
        var conflicts = 0
        for (entity in pending) {
            _progress.emit(SyncProgress.Pushing(entity.operationId ?: "", completed, total))
            val result = processEntity(entity)
            if (result) completed++ else conflicts++
        }
        _progress.emit(SyncProgress.Completed(completed, 0, conflicts))
    }

    private suspend fun pullAndSync() {
        var cursor = pullCursor
        var pulled = 0
        do {
            _progress.emit(SyncProgress.Pulling(cursor))
            val result = transportAdapter.pull(cursor)
            for (entity in result.entities) {
                val localEntity = storageAdapter.read(entity.id)
                val resolvedEntity = if (localEntity != null) {
                    conflictResolver.resolve(localEntity, entity)
                } else {
                    entity
                }
                storageAdapter.write(resolvedEntity)
                pulled++
            }
            cursor = result.nextCursor
        } while (cursor != null)
        pullCursor = null
        _progress.emit(SyncProgress.Completed(0, pulled, 0))
    }

    private suspend fun processEntity(entity: SyncEntity): Boolean {
        val op = SyncOperation(
            operationId = entity.operationId ?: Uuid.random().toString(),
            entityId = entity.id,
            type = entity.pendingOperationType!!,
            attempt = entity.operationAttempt,
            payload = entity.payload,
        )

        return try {
            val result = transportAdapter.push(op)
            when (result) {
                is PushResult.Success -> {
                    storageAdapter.write(entity.copy(
                        syncState = SyncState.Synced,
                        version = result.version,
                        pendingOperationType = null,
                        operationId = null,
                        operationAttempt = 0,
                    ))
                    true
                }
                is PushResult.Conflict -> {
                    val localEntity = storageAdapter.read(entity.id)
                    val resolvedEntity = if (localEntity != null) {
                        conflictResolver.resolve(localEntity, result.remoteEntity)
                    } else {
                        result.remoteEntity
                    }
                    storageAdapter.write(resolvedEntity.copy(
                        syncState = SyncState.Synced,
                        pendingOperationType = null,
                        operationId = null,
                        operationAttempt = 0,
                    ))
                    false
                }
                is PushResult.Error -> {
                    val attempt = entity.operationAttempt + 1
                    if (retryPolicy.shouldDeadLetter(attempt, result.error)) {
                        storageAdapter.write(entity.copy(
                            syncState = SyncState.Failed,
                            pendingOperationType = null,
                            operationId = null,
                            operationAttempt = 0,
                        ))
                        _progress.emit(SyncProgress.Error(entity.operationId ?: "", result.error))
                    } else {
                        storageAdapter.write(entity.copy(operationAttempt = attempt))
                        val delayDuration = retryPolicy.nextDelay(attempt)
                        scope.launch {
                            delay(delayDuration)
                            commandChannel.send(SyncCommand.TriggerSync)
                        }
                    }
                    false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val attempt = entity.operationAttempt + 1
            if (retryPolicy.shouldDeadLetter(attempt, SyncError.Unknown(e))) {
                storageAdapter.write(entity.copy(
                    syncState = SyncState.Failed,
                    pendingOperationType = null,
                    operationId = null,
                    operationAttempt = 0,
                ))
                _progress.emit(SyncProgress.Error(entity.operationId ?: "", SyncError.Unknown(e)))
            } else {
                storageAdapter.write(entity.copy(operationAttempt = attempt))
                val delayDuration = retryPolicy.nextDelay(attempt)
                scope.launch {
                    delay(delayDuration)
                    commandChannel.send(SyncCommand.TriggerSync)
                }
            }
            false
        }
    }

    fun stop() {
        engineJob?.cancel()
    }
}
