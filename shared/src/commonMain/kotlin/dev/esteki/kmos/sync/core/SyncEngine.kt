package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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
        pullCursor = cursor
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
                    operationQueue.markFailed(op.operationId)
                }
            }
        } catch (e: Exception) {
            operationQueue.markFailed(op.operationId)
        }
    }

    fun stop() {
        engineJob?.cancel()
    }
}
