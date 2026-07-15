package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncProgress
import dev.esteki.kmos.sync.core.model.SyncState
import dev.esteki.kmos.sync.trigger.SyncTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.Uuid

class SyncClient private constructor(
    private val scope: CoroutineScope,
    private val storageAdapter: StorageAdapter,
    private val commandChannel: Channel<SyncCommand>,
    private val engine: SyncEngine,
    private val syncTrigger: SyncTrigger?,
) {
    private val _failedOperations = MutableStateFlow<List<SyncEntity>>(emptyList())
    val failedOperations: StateFlow<List<SyncEntity>> = _failedOperations.asStateFlow()

    val progress: SharedFlow<SyncProgress> = engine.progress

    fun start() {
        engine.start()
        scope.launch {
            refreshFailedOperations()
        }
        syncTrigger?.onForeground()
    }

    fun trigger() {
        scope.launch {
            commandChannel.send(SyncCommand.TriggerSync)
            refreshFailedOperations()
        }
    }

    fun stop() {
        syncTrigger?.stopInterval()
        engine.stop()
    }

    fun cancelPending() {
        scope.launch {
            commandChannel.send(SyncCommand.Cancel)
        }
    }

    fun enqueue(operation: SyncOperation) {
        scope.launch {
            commandChannel.send(SyncCommand.Enqueue(operation))
        }
    }

    fun retry(entity: SyncEntity) {
        val operation = SyncOperation(
            operationId = Uuid.random().toString(),
            entityId = entity.id,
            type = OperationType.Update,
            attempt = 0,
            payload = entity.payload,
        )
        scope.launch {
            commandChannel.send(SyncCommand.Enqueue(operation))
            refreshFailedOperations()
        }
    }

    fun discard(entity: SyncEntity) {
        scope.launch {
            storageAdapter.write(entity.copy(syncState = SyncState.LocalOnly))
            refreshFailedOperations()
        }
    }

    fun <T : Any> repository(
        serialize: (T) -> SyncEntity,
        deserialize: (SyncEntity) -> T,
    ): SyncRepository<T> {
        return object : SyncRepository<T> {
            override suspend fun read(id: String): T? {
                val entity = storageAdapter.read(id) ?: return null
                if (entity.deleted) return null
                return deserialize(entity)
            }

            override suspend fun readAll(): List<T> {
                val pending = storageAdapter.queryPending()
                val failed = storageAdapter.queryFailed()
                return (pending + failed).filter { !it.deleted }.map { deserialize(it) }
            }

            override suspend fun upsert(value: T) {
                val entity = serialize(value)
                val existing = storageAdapter.read(entity.id)
                val now = Clock.System.now()
                val writeEntity = if (existing != null) {
                    entity.copy(
                        version = existing.version,
                        updatedAt = now,
                    )
                } else {
                    entity.copy(updatedAt = now)
                }
                storageAdapter.write(writeEntity)
                val type = if (existing == null) OperationType.Create else OperationType.Update
                val operation = SyncOperation(
                    operationId = Uuid.random().toString(),
                    entityId = writeEntity.id,
                    type = type,
                    attempt = 0,
                    payload = writeEntity.payload,
                )
                enqueue(operation)
            }

            override suspend fun delete(id: String) {
                val existing = storageAdapter.read(id)
                if (existing != null) {
                    storageAdapter.write(existing.copy(deleted = true, syncState = SyncState.PendingUpload))
                    val operation = SyncOperation(
                        operationId = Uuid.random().toString(),
                        entityId = id,
                        type = OperationType.Delete,
                        attempt = 0,
                        payload = existing.payload,
                    )
                    enqueue(operation)
                }
            }

            override fun observeAll(): Flow<List<T>> = flow {
                val pending = storageAdapter.queryPending()
                val failed = storageAdapter.queryFailed()
                emit((pending + failed).filter { !it.deleted }.map { deserialize(it) })
            }
        }
    }

    private suspend fun refreshFailedOperations() {
        _failedOperations.value = storageAdapter.queryFailed()
    }

    class Builder {
        private var storageAdapter: StorageAdapter? = null
        private var transportAdapter: TransportAdapter? = null
        private var retryPolicy: RetryPolicy? = null
        private var conflictResolver: ConflictResolver<SyncEntity>? = null
        private var syncTrigger: SyncTrigger? = null

        fun storage(adapter: StorageAdapter) = apply { this.storageAdapter = adapter }
        fun transport(adapter: TransportAdapter) = apply { this.transportAdapter = adapter }
        fun retry(policy: RetryPolicy) = apply { this.retryPolicy = policy }
        fun conflictResolver(resolver: ConflictResolver<SyncEntity>) = apply { this.conflictResolver = resolver }
        fun trigger(trigger: SyncTrigger?) = apply { this.syncTrigger = trigger }

        fun build(scope: CoroutineScope): SyncClient {
            val storage = requireNotNull(storageAdapter) { "StorageAdapter is required" }
            val transport = requireNotNull(transportAdapter) { "TransportAdapter is required" }
            val retry = retryPolicy ?: ExponentialBackoffRetryPolicy()
            val resolver = conflictResolver ?: LastWriteWinsConflictResolver()

            val commandChannel = Channel<SyncCommand>(Channel.BUFFERED)
            val queue = InMemoryOperationQueue(retry)
            val engine = SyncEngine(
                scope = scope,
                commandChannel = commandChannel,
                operationQueue = queue,
                storageAdapter = storage,
                transportAdapter = transport,
                retryPolicy = retry,
                conflictResolver = resolver,
            )

            return SyncClient(
                scope = scope,
                storageAdapter = storage,
                commandChannel = commandChannel,
                engine = engine,
                syncTrigger = syncTrigger,
            )
        }
    }

    companion object {
        fun build(scope: CoroutineScope, block: Builder.() -> Unit): SyncClient {
            return Builder().apply(block).build(scope)
        }
    }
}
