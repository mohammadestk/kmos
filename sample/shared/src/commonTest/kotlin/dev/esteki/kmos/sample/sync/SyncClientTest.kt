package dev.esteki.kmos.sample.sync

import dev.esteki.kmos.sync.core.ExponentialBackoffRetryPolicy
import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import dev.esteki.kmos.sync.testing.FakeStorageAdapter
import dev.esteki.kmos.sync.testing.FakeTransportAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

class SyncClientTest {

    @Test
    fun buildWithRequiredFields() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        assertNotNull(client)
    }

    @Test
    fun buildWithAllFields() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()
        val retry = ExponentialBackoffRetryPolicy()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
            retry(retry)
        }

        assertNotNull(client)
    }

    @Test
    fun startDoesNotThrow() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        client.start()
        client.stop()
    }

    @Test
    fun triggerDoesNotThrow() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        client.start()
        client.trigger()
        client.stop()
    }

    @Test
    fun failedOperationsInitiallyEmpty() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        assertEquals(emptyList(), client.failedOperations.value)
    }

    @Test
    fun retryEnqueuesOperation() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        client.start()

        val entity = SyncEntity(
            id = "entity-1",
            version = 0L,
            updatedAt = Instant.fromEpochMilliseconds(0L),
            deleted = false,
            syncState = SyncState.Failed,
            payload = byteArrayOf(1),
        )
        storage.write(entity)

        client.retry(entity)
        delay(100)
        client.stop()
    }

    @Test
    fun discardRemovesEntityFromFailed() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        client.start()

        val entity = SyncEntity(
            id = "entity-1",
            version = 0L,
            updatedAt = Instant.fromEpochMilliseconds(0L),
            deleted = false,
            syncState = SyncState.Failed,
            payload = byteArrayOf(1),
        )
        storage.write(entity)

        client.discard(entity)
        delay(100)
        client.stop()
    }

    @Test
    fun repositoryWritesToStorageAndEnqueues() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        val repo = client.repository<String>(
            serialize = { id ->
                SyncEntity(
                    id = id,
                    version = 0L,
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                    deleted = false,
                    syncState = SyncState.PendingUpload,
                    payload = id.toByteArray(),
                )
            },
            deserialize = { entity -> entity.payload.decodeToString() },
        )

        repo.upsert("item-1")
        val read = repo.read("item-1")
        assertEquals("item-1", read)

        val all = repo.readAll()
        assertEquals(1, all.size)
        assertEquals("item-1", all[0])
    }

    @Test
    fun repositoryDeleteMarksDeleted() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        val repo = client.repository<String>(
            serialize = { id ->
                SyncEntity(
                    id = id,
                    version = 0L,
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                    deleted = false,
                    syncState = SyncState.PendingUpload,
                    payload = id.toByteArray(),
                )
            },
            deserialize = { entity -> entity.payload.decodeToString() },
        )

        repo.upsert("item-1")
        repo.delete("item-1")
        val read = repo.read("item-1")
        assertEquals(null, read)
    }
}
