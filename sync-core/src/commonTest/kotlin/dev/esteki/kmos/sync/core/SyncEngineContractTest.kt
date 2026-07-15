package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import dev.esteki.kmos.sync.testing.FakeStorageAdapter
import dev.esteki.kmos.sync.testing.FakeTransportAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class SyncEngineContractTest {

    protected abstract fun createStorage(): FakeStorageAdapter
    protected abstract fun createTransport(): FakeTransportAdapter

    protected class EngineWithContext(
        val engine: SyncEngine,
        val commandChannel: Channel<SyncCommand>,
    )

    protected fun TestScope.runSuccessfulPushUpdatesStorageToSynced() = runTest {
        val storage = createStorage()
        val transport = createTransport()
        transport.pushResult = PushResult.Success(version = 1L)

        val ctx = createEngineWithContext(this, storage, transport)
        ctx.engine.start()

        storage.addEntity(createEntity("entity-1", syncState = SyncState.PendingUpload))
        ctx.commandChannel.send(SyncCommand.TriggerSync)

        advanceUntilIdle()

        val written = storage.getEntity("entity-1")
        assertNotNull(written)
        assertEquals(SyncState.Synced, written.syncState)
        assertEquals(1L, written.version)
        assertEquals(null, written.pendingOperationType)

        ctx.engine.stop()
    }

    protected fun TestScope.runConflictStoresRemoteEntity() = runTest {
        val storage = createStorage()
        val transport = createTransport()
        val remoteEntity = createEntity("entity-1", version = 2L, syncState = SyncState.Synced)
        transport.pushResult = PushResult.Conflict(remoteEntity)

        val ctx = createEngineWithContext(this, storage, transport)
        ctx.engine.start()

        storage.addEntity(createEntity("entity-1", version = 1L, syncState = SyncState.PendingUpload))
        ctx.commandChannel.send(SyncCommand.TriggerSync)

        advanceUntilIdle()

        val written = storage.getEntity("entity-1")
        assertNotNull(written)
        assertEquals(SyncState.Synced, written.syncState)
        assertEquals(2L, written.version)
        assertEquals(null, written.pendingOperationType)

        ctx.engine.stop()
    }

    protected fun TestScope.runDeadLetterAfterMaxAttempts() = runTest {
        val storage = createStorage()
        val transport = createTransport()
        transport.pushResult = PushResult.Error(dev.esteki.kmos.sync.core.model.SyncError.NetworkTimeout)

        val ctx = createEngineWithContext(this, storage, transport)
        ctx.engine.start()

        storage.addEntity(createEntity("entity-1", syncState = SyncState.PendingUpload, operationAttempt = 2))
        ctx.commandChannel.send(SyncCommand.TriggerSync)

        advanceUntilIdle()

        val written = storage.getEntity("entity-1")
        assertNotNull(written)
        assertEquals(SyncState.Failed, written.syncState)
        assertEquals(null, written.pendingOperationType)

        ctx.engine.stop()
    }

    protected fun TestScope.runRetryAfterFailureRequeues() = runTest {
        val storage = createStorage()
        val transport = createTransport()
        transport.pushResult = PushResult.Error(dev.esteki.kmos.sync.core.model.SyncError.NetworkTimeout)

        val ctx = createEngineWithContext(this, storage, transport)
        ctx.engine.start()

        storage.addEntity(createEntity("entity-1", syncState = SyncState.PendingUpload))
        ctx.commandChannel.send(SyncCommand.TriggerSync)

        advanceUntilIdle()

        val written = storage.getEntity("entity-1")
        assertNotNull(written)
        assertEquals(1, written.operationAttempt)
        assertEquals(OperationType.Update, written.pendingOperationType)

        ctx.engine.stop()
    }

    protected fun TestScope.runPullAndSyncStoresEntities() = runTest {
        val storage = createStorage()
        val transport = createTransport()
        val pulledEntity = createEntity("pulled-1", version = 1L, syncState = SyncState.Synced)
        transport.pullResult = dev.esteki.kmos.sync.core.model.PullResult(
            entities = listOf(pulledEntity),
            nextCursor = null,
        )

        val ctx = createEngineWithContext(this, storage, transport)
        ctx.engine.start()

        ctx.commandChannel.send(SyncCommand.PullAndSync)

        advanceUntilIdle()

        val written = storage.getEntity("pulled-1")
        assertNotNull(written)
        assertEquals("pulled-1", written.id)
        assertEquals(1L, written.version)

        ctx.engine.stop()
    }

    protected fun TestScope.runCancelStopsEngine() = runTest {
        val storage = createStorage()
        val transport = createTransport()
        transport.pushResult = PushResult.Success(version = 1L)

        val ctx = createEngineWithContext(this, storage, transport)
        ctx.engine.start()

        ctx.commandChannel.send(SyncCommand.Cancel)

        advanceUntilIdle()

        ctx.engine.stop()
    }

    protected fun createEngineWithContext(
        scope: TestScope,
        storage: FakeStorageAdapter,
        transport: FakeTransportAdapter,
    ): EngineWithContext {
        val retryPolicy = ExponentialBackoffRetryPolicy(maxAttempts = 3)
        val commandChannel = Channel<SyncCommand>(Channel.BUFFERED)

        val engine = SyncEngine(
            scope = scope,
            commandChannel = commandChannel,
            storageAdapter = storage,
            transportAdapter = transport,
            retryPolicy = retryPolicy,
            conflictResolver = LastWriteWinsConflictResolver(),
        )

        return EngineWithContext(engine, commandChannel)
    }

    protected fun createEntity(
        id: String,
        version: Long = 1L,
        syncState: SyncState = SyncState.LocalOnly,
        operationAttempt: Int = 0,
    ) = SyncEntity(
        id = id,
        version = version,
        updatedAt = Instant.fromEpochMilliseconds(0L),
        deleted = false,
        syncState = syncState,
        payload = byteArrayOf(),
        pendingOperationType = if (syncState == SyncState.PendingUpload) OperationType.Update else null,
        operationId = if (syncState == SyncState.PendingUpload) "op-$id" else null,
        operationAttempt = operationAttempt,
    )
}
