package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SyncClient private constructor(
    private val scope: CoroutineScope,
    private val storageAdapter: StorageAdapter,
    private val transportAdapter: TransportAdapter,
    private val retryPolicy: RetryPolicy,
    private val commandChannel: Channel<SyncCommand>,
    private val engine: SyncEngine,
) {
    private val _failedOperations = MutableStateFlow<List<SyncOperation>>(emptyList())
    val failedOperations: StateFlow<List<SyncOperation>> = _failedOperations.asStateFlow()

    fun start() {
        engine.start()
        scope.launch {
            refreshFailedOperations()
        }
    }

    fun trigger() {
        scope.launch {
            commandChannel.send(SyncCommand.TriggerSync)
            refreshFailedOperations()
        }
    }

    fun <T : Any> repository(
        observe: suspend (id: String) -> Flow<T?>,
        observeAll: suspend () -> Flow<List<T>>,
        upsert: suspend (T) -> Unit,
        delete: suspend (String) -> Unit,
    ): SyncRepository<T> {
        return object : SyncRepository<T> {
            override fun observe(id: String): Flow<T?> = emptyFlow()
            override fun observeAll(): Flow<List<T>> = emptyFlow()
            override suspend fun upsert(value: T) = upsert(value)
            override suspend fun delete(id: String) = delete(id)
        }
    }

    fun stop() {
        engine.stop()
    }

    private suspend fun refreshFailedOperations() {
        val pending = storageAdapter.queryPending()
        val failed = pending.filter { it.syncState == SyncState.Failed }
        _failedOperations.value = failed.map { entity ->
            SyncOperation(
                operationId = Uuid.random().toString(),
                entityId = entity.id,
                type = dev.esteki.kmos.sync.core.model.OperationType.Update,
                attempt = 0,
                payload = entity.payload,
            )
        }
    }

    class Builder {
        private var storageAdapter: StorageAdapter? = null
        private var transportAdapter: TransportAdapter? = null
        private var retryPolicy: RetryPolicy? = null
        private var conflictResolver: ConflictResolver<SyncEntity>? = null

        fun storage(adapter: StorageAdapter) = apply { this.storageAdapter = adapter }
        fun transport(adapter: TransportAdapter) = apply { this.transportAdapter = adapter }
        fun retry(policy: RetryPolicy) = apply { this.retryPolicy = policy }
        fun conflictResolver(resolver: ConflictResolver<SyncEntity>) = apply { this.conflictResolver = resolver }

        fun build(scope: CoroutineScope): SyncClient {
            val storage = requireNotNull(storageAdapter) { "StorageAdapter is required" }
            val transport = requireNotNull(transportAdapter) { "TransportAdapter is required" }
            val retry = retryPolicy ?: ExponentialBackoffRetryPolicy()
            val resolver = conflictResolver ?: LastWriteWinsConflictResolver()

            val commandChannel = Channel<SyncCommand>(Channel.BUFFERED)
            val queue = OperationQueue(retry)
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
                transportAdapter = transport,
                retryPolicy = retry,
                commandChannel = commandChannel,
                engine = engine,
            )
        }
    }

    companion object {
        fun build(scope: CoroutineScope, block: Builder.() -> Unit): SyncClient {
            return Builder().apply(block).build(scope)
        }
    }
}
