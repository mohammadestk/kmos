package dev.esteki.kmos.sync.core

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
import kotlinx.coroutines.launch
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

    fun <T : Any> repository(
        observe: (id: String) -> Flow<T?>,
        observeAll: () -> Flow<List<T>>,
        upsert: suspend (T) -> Unit,
        delete: suspend (String) -> Unit,
    ): SyncRepository<T> {
        return object : SyncRepository<T> {
            override fun observe(id: String): Flow<T?> = observe(id)
            override fun observeAll(): Flow<List<T>> = observeAll()
            override suspend fun upsert(value: T) = upsert(value)
            override suspend fun delete(id: String) = delete(id)
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

    fun retry(entity: SyncEntity) {
        val operation = SyncOperation(
            operationId = Uuid.random().toString(),
            entityId = entity.id,
            type = dev.esteki.kmos.sync.core.model.OperationType.Update,
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
