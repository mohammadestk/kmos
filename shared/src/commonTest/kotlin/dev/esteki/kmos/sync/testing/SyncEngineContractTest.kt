package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.ExponentialBackoffRetryPolicy
import dev.esteki.kmos.sync.core.LastWriteWinsConflictResolver
import dev.esteki.kmos.sync.core.OperationQueue
import dev.esteki.kmos.sync.core.SyncCommand
import dev.esteki.kmos.sync.core.SyncEngine
import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
abstract class SyncEngineContractTest {

    protected abstract fun createStorage(): FakeStorageAdapter
    protected abstract fun createTransport(): FakeTransportAdapter

    @Test
    fun successfulPushUpdatesStorageToSynced() = runTest {
        val storage = createStorage()
        val transport = createTransport()
        transport.pushResult = PushResult.Success(version = 1L)

        val engine = createEngine(this, storage, transport)
        engine.start()

        storage.addEntity(createEntity("entity-1", syncState = SyncState.PendingUpload))
        sendCommand(engine, SyncCommand.Enqueue(createOperation("op-1", "entity-1")))
        sendCommand(engine, SyncCommand.TriggerSync)

        advanceUntilIdle()

        val written = storage.getEntity("entity-1")
        assertNotNull(written)
        assertEquals(SyncState.Synced, written.syncState)
        assertEquals(1L, written.version)

        engine.stop()
    }

    @Test
    fun conflictStoresRemoteEntity() = runTest {
        val storage = createStorage()
        val transport = createTransport()
        val remoteEntity = createEntity("entity-1", version = 2L, syncState = SyncState.Synced)
        transport.pushResult = PushResult.Conflict(remoteEntity)

        val engine = createEngine(this, storage, transport)
        engine.start()

        storage.addEntity(createEntity("entity-1", version = 1L, syncState = SyncState.PendingUpload))
        sendCommand(engine, SyncCommand.Enqueue(createOperation("op-1", "entity-1")))
        sendCommand(engine, SyncCommand.TriggerSync)

        advanceUntilIdle()

        val written = storage.getEntity("entity-1")
        assertNotNull(written)
        assertEquals(SyncState.Conflict, written.syncState)
        assertEquals(2L, written.version)

        engine.stop()
    }

    protected fun createEngine(
        scope: TestScope,
        storage: FakeStorageAdapter,
        transport: FakeTransportAdapter,
    ): SyncEngine {
        val retryPolicy = ExponentialBackoffRetryPolicy(maxAttempts = 3)
        val commandChannel = Channel<SyncCommand>(Channel.BUFFERED)
        val queue = OperationQueue(retryPolicy)

        return SyncEngine(
            scope = scope,
            commandChannel = commandChannel,
            operationQueue = queue,
            storageAdapter = storage,
            transportAdapter = transport,
            retryPolicy = retryPolicy,
            conflictResolver = LastWriteWinsConflictResolver(),
        )
    }

    protected suspend fun sendCommand(engine: SyncEngine, command: SyncCommand) {
        // This is a placeholder - in real tests, we'd need access to the command channel
        // For now, we'll use a different approach
    }

    protected fun createEntity(
        id: String,
        version: Long = 1L,
        syncState: SyncState = SyncState.LocalOnly,
    ) = SyncEntity(
        id = id,
        version = version,
        updatedAt = Instant.fromEpochMilliseconds(0L),
        deleted = false,
        syncState = syncState,
        payload = byteArrayOf(),
    )

    protected fun createOperation(
        operationId: String,
        entityId: String,
    ) = SyncOperation(
        operationId = operationId,
        entityId = entityId,
        type = OperationType.Update,
        attempt = 0,
        payload = byteArrayOf(),
    )
}
