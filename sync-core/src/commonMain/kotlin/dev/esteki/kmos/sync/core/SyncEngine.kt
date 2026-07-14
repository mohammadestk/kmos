package dev.esteki.kmos.sync.core

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

class SyncEngine(
    private val scope: CoroutineScope,
    private val commandChannel: Channel<SyncCommand>,
    private val operationQueue: OperationQueue,
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
                    is SyncCommand.Enqueue -> {
                        operationQueue.enqueue(command.operation)
                    }
                    is SyncCommand.TriggerSync -> {
                        drainQueue()
                    }
                    is SyncCommand.PullAndSync -> {
                        pullAndSync()
                    }
                    is SyncCommand.Cancel -> {
                        break
                    }
                }
            }
        }
    }

    private suspend fun drainQueue() {
        val pending = operationQueue.dequeuePending()
        val total = pending.size
        var completed = 0
        var conflicts = 0
        for (op in pending) {
            _progress.emit(SyncProgress.Pushing(op.operationId, completed, total))
            val result = processOperation(op)
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

    private suspend fun processOperation(op: SyncOperation): Boolean {
        return try {
            val result = transportAdapter.push(op)
            when (result) {
                is PushResult.Success -> {
                    val entity = storageAdapter.read(op.entityId)
                    if (entity != null) {
                        storageAdapter.write(entity.copy(syncState = SyncState.Synced, version = result.version))
                    }
                    operationQueue.markDone(op.operationId)
                    true
                }
                is PushResult.Conflict -> {
                    val localEntity = storageAdapter.read(op.entityId)
                    val resolvedEntity = if (localEntity != null) {
                        conflictResolver.resolve(localEntity, result.remoteEntity)
                    } else {
                        result.remoteEntity
                    }
                    storageAdapter.write(resolvedEntity.copy(syncState = SyncState.Synced))
                    operationQueue.markDone(op.operationId)
                    false
                }
                is PushResult.Error -> {
                    val attempt = op.attempt + 1
                    if (retryPolicy.shouldDeadLetter(attempt, result.error)) {
                        operationQueue.markFailed(op.operationId, result.error)
                        val entity = storageAdapter.read(op.entityId)
                        if (entity != null) {
                            storageAdapter.write(entity.copy(syncState = SyncState.Failed))
                        }
                        _progress.emit(SyncProgress.Error(op.operationId, result.error))
                    } else {
                        val delay = retryPolicy.nextDelay(attempt)
                        operationQueue.remove(op.operationId)
                        scope.launch {
                            delay(delay)
                            commandChannel.send(SyncCommand.Enqueue(op.copy(attempt = attempt)))
                        }
                    }
                    false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val attempt = op.attempt + 1
            if (retryPolicy.shouldDeadLetter(attempt, SyncError.Unknown(e))) {
                operationQueue.markFailed(op.operationId, SyncError.Unknown(e))
                val entity = storageAdapter.read(op.entityId)
                if (entity != null) {
                    storageAdapter.write(entity.copy(syncState = SyncState.Failed))
                }
                _progress.emit(SyncProgress.Error(op.operationId, SyncError.Unknown(e)))
            } else {
                val delay = retryPolicy.nextDelay(attempt)
                operationQueue.remove(op.operationId)
                scope.launch {
                    delay(delay)
                    commandChannel.send(SyncCommand.Enqueue(op.copy(attempt = attempt)))
                }
            }
            false
        }
    }

    fun stop() {
        engineJob?.cancel()
    }
}
