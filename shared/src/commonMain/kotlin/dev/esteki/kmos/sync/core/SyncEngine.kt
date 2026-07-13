package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
        for (op in pending) {
            processOperation(op)
        }
    }

    private suspend fun pullAndSync() {
        // Fetch all pages using cursor-based pagination
        var cursor = pullCursor
        do {
            val result = transportAdapter.pull(cursor)
            for (entity in result.entities) {
                val localEntity = storageAdapter.read(entity.id)
                val resolvedEntity = if (localEntity != null) {
                    conflictResolver.resolve(localEntity, entity)
                } else {
                    entity
                }
                storageAdapter.write(resolvedEntity)
            }
            cursor = result.nextCursor
        } while (cursor != null)
        // Reset cursor after fetching all pages
        pullCursor = null
    }

    private suspend fun processOperation(op: dev.esteki.kmos.sync.core.model.SyncOperation) {
        try {
            val result = transportAdapter.push(op)
            when (result) {
                is PushResult.Success -> {
                    val entity = storageAdapter.read(op.entityId)
                    if (entity != null) {
                        storageAdapter.write(entity.copy(syncState = SyncState.Synced, version = result.version))
                    }
                    operationQueue.markDone(op.operationId)
                }
                is PushResult.Conflict -> {
                    // TODO: Consider operation payload when resolving conflicts
                    val localEntity = storageAdapter.read(op.entityId)
                    val resolvedEntity = if (localEntity != null) {
                        conflictResolver.resolve(localEntity, result.remoteEntity)
                    } else {
                        result.remoteEntity
                    }
                    storageAdapter.write(resolvedEntity.copy(syncState = SyncState.Synced))
                    operationQueue.markDone(op.operationId)
                }
                is PushResult.Error -> {
                    val attempt = op.attempt + 1
                    if (retryPolicy.shouldDeadLetter(attempt, result.error)) {
                        // Dead-letter: mark operation as failed and update entity syncState
                        operationQueue.markFailed(op.operationId, result.error)
                        val entity = storageAdapter.read(op.entityId)
                        if (entity != null) {
                            storageAdapter.write(entity.copy(syncState = SyncState.Failed))
                        }
                    } else {
                        val delay = retryPolicy.nextDelay(attempt)
                        operationQueue.remove(op.operationId)
                        scope.launch {
                            delay(delay)
                            commandChannel.send(SyncCommand.Enqueue(op.copy(attempt = attempt)))
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val attempt = op.attempt + 1
            if (retryPolicy.shouldDeadLetter(attempt, SyncError.Unknown(e))) {
                // Dead-letter: mark operation as failed and update entity syncState
                operationQueue.markFailed(op.operationId, SyncError.Unknown(e))
                val entity = storageAdapter.read(op.entityId)
                if (entity != null) {
                    storageAdapter.write(entity.copy(syncState = SyncState.Failed))
                }
            } else {
                val delay = retryPolicy.nextDelay(attempt)
                operationQueue.remove(op.operationId)
                scope.launch {
                    delay(delay)
                    commandChannel.send(SyncCommand.Enqueue(op.copy(attempt = attempt)))
                }
            }
        }
    }

    fun stop() {
        engineJob?.cancel()
    }
}
