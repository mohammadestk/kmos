package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SyncEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun successfulPushUpdatesStorageToSynced() = runTest {
        val storage = FakeStorage()
        val transport = FakeTransport()
        transport.pushResult = PushResult.Success(version = 1L)

        val commandChannel = Channel<SyncCommand>(Channel.BUFFERED)
        val retryPolicy = ExponentialBackoffRetryPolicy(maxAttempts = 3)
        val queue = InMemoryOperationQueue(retryPolicy)

        val engine = SyncEngine(
            scope = testScope,
            commandChannel = commandChannel,
            operationQueue = queue,
            storageAdapter = storage,
            transportAdapter = transport,
            retryPolicy = retryPolicy,
            conflictResolver = LastWriteWinsConflictResolver(),
        )
        engine.start()

        val entity = SyncEntity(
            id = "entity-1",
            version = 0L,
            updatedAt = Instant.fromEpochMilliseconds(0L),
            deleted = false,
            syncState = SyncState.PendingUpload,
            payload = byteArrayOf(1),
        )
        storage.entities["entity-1"] = entity

        val op = SyncOperation(
            operationId = "op-1",
            entityId = "entity-1",
            type = OperationType.Update,
            attempt = 0,
            payload = byteArrayOf(1),
        )

        commandChannel.send(SyncCommand.Enqueue(op))
        commandChannel.send(SyncCommand.TriggerSync)

        testScope.testScheduler.advanceUntilIdle()

        val written = storage.entities["entity-1"]
        assertNotNull(written)
        assertEquals(SyncState.Synced, written.syncState)
        assertEquals(1L, written.version)

        engine.stop()
    }

    @Test
    fun conflictTriggersResolver() = runTest {
        val storage = FakeStorage()
        val transport = FakeTransport()
        val remoteEntity = SyncEntity(
            id = "entity-1",
            version = 2L,
            updatedAt = Instant.fromEpochMilliseconds(2000L),
            deleted = false,
            syncState = SyncState.Synced,
            payload = byteArrayOf(2),
        )
        transport.pushResult = PushResult.Conflict(remoteEntity)

        val commandChannel = Channel<SyncCommand>(Channel.BUFFERED)
        val retryPolicy = ExponentialBackoffRetryPolicy(maxAttempts = 3)
        val queue = InMemoryOperationQueue(retryPolicy)

        val engine = SyncEngine(
            scope = testScope,
            commandChannel = commandChannel,
            operationQueue = queue,
            storageAdapter = storage,
            transportAdapter = transport,
            retryPolicy = retryPolicy,
            conflictResolver = LastWriteWinsConflictResolver(),
        )
        engine.start()

        val entity = SyncEntity(
            id = "entity-1",
            version = 1L,
            updatedAt = Instant.fromEpochMilliseconds(1000L),
            deleted = false,
            syncState = SyncState.PendingUpload,
            payload = byteArrayOf(1),
        )
        storage.entities["entity-1"] = entity

        val op = SyncOperation(
            operationId = "op-1",
            entityId = "entity-1",
            type = OperationType.Update,
            attempt = 0,
            payload = byteArrayOf(1),
        )

        commandChannel.send(SyncCommand.Enqueue(op))
        commandChannel.send(SyncCommand.TriggerSync)

        testScope.testScheduler.advanceUntilIdle()

        val written = storage.entities["entity-1"]
        assertNotNull(written)
        assertEquals(SyncState.Synced, written.syncState)

        engine.stop()
    }

    @Test
    fun transientFailureTriggersRetry() = runTest {
        val storage = FakeStorage()
        val transport = FakeTransport()
        transport.pushResult = PushResult.Error(SyncError.NetworkTimeout)

        val commandChannel = Channel<SyncCommand>(Channel.BUFFERED)
        val retryPolicy = ExponentialBackoffRetryPolicy(maxAttempts = 3)
        val queue = InMemoryOperationQueue(retryPolicy)

        val engine = SyncEngine(
            scope = testScope,
            commandChannel = commandChannel,
            operationQueue = queue,
            storageAdapter = storage,
            transportAdapter = transport,
            retryPolicy = retryPolicy,
            conflictResolver = LastWriteWinsConflictResolver(),
        )
        engine.start()

        val entity = SyncEntity(
            id = "entity-1",
            version = 0L,
            updatedAt = Instant.fromEpochMilliseconds(0L),
            deleted = false,
            syncState = SyncState.PendingUpload,
            payload = byteArrayOf(1),
        )
        storage.entities["entity-1"] = entity

        val op = SyncOperation(
            operationId = "op-1",
            entityId = "entity-1",
            type = OperationType.Update,
            attempt = 0,
            payload = byteArrayOf(1),
        )

        commandChannel.send(SyncCommand.Enqueue(op))
        commandChannel.send(SyncCommand.TriggerSync)

        testScope.testScheduler.advanceUntilIdle()

        val pending = queue.dequeuePending()
        assertTrue(pending.isNotEmpty())
        assertEquals(1, pending[0].attempt)

        engine.stop()
    }

    @Test
    fun pullAndSyncStoresEntities() = runTest {
        val storage = FakeStorage()
        val transport = FakeTransport()
        val pulledEntity = SyncEntity(
            id = "pulled-1",
            version = 1L,
            updatedAt = Instant.fromEpochMilliseconds(1000L),
            deleted = false,
            syncState = SyncState.Synced,
            payload = byteArrayOf(1),
        )
        transport.pullResult = dev.esteki.kmos.sync.core.model.PullResult(
            entities = listOf(pulledEntity),
            nextCursor = null,
        )

        val commandChannel = Channel<SyncCommand>(Channel.BUFFERED)
        val retryPolicy = ExponentialBackoffRetryPolicy(maxAttempts = 3)
        val queue = InMemoryOperationQueue(retryPolicy)

        val engine = SyncEngine(
            scope = testScope,
            commandChannel = commandChannel,
            operationQueue = queue,
            storageAdapter = storage,
            transportAdapter = transport,
            retryPolicy = retryPolicy,
            conflictResolver = LastWriteWinsConflictResolver(),
        )
        engine.start()

        commandChannel.send(SyncCommand.PullAndSync)

        testScope.testScheduler.advanceUntilIdle()

        val written = storage.entities["pulled-1"]
        assertNotNull(written)
        assertEquals("pulled-1", written.id)
        assertEquals(1L, written.version)

        engine.stop()
    }

    private class FakeStorage : StorageAdapter {
        val entities = mutableMapOf<String, SyncEntity>()

        override suspend fun read(id: String): SyncEntity? = entities[id]
        override suspend fun write(entity: SyncEntity) {
            entities[entity.id] = entity
        }
        override suspend fun delete(id: String) {
            entities.remove(id)
        }
        override suspend fun queryPending(): List<SyncEntity> =
            entities.values.filter { it.syncState == SyncState.PendingUpload }
        override suspend fun queryFailed(): List<SyncEntity> =
            entities.values.filter { it.syncState == SyncState.Failed }
    }

    private class FakeTransport : TransportAdapter {
        var pushResult: PushResult = PushResult.Success(1L)
        var pullResult: dev.esteki.kmos.sync.core.model.PullResult = dev.esteki.kmos.sync.core.model.PullResult(emptyList(), null)

        override suspend fun push(op: SyncOperation): PushResult = pushResult
        override suspend fun pull(cursor: String?): dev.esteki.kmos.sync.core.model.PullResult = pullResult
    }
}
