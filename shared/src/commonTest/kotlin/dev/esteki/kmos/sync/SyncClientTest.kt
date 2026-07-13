package dev.esteki.kmos.sync

import dev.esteki.kmos.sync.core.ExponentialBackoffRetryPolicy
import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.testing.FakeStorageAdapter
import dev.esteki.kmos.sync.testing.FakeTransportAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
    fun syncClientBuilderFunction() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = syncClient(this) {
            storage(storage)
            transport(transport)
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
    fun repositoryDelegatesToLambdas() = runTest {
        val storage = FakeStorageAdapter()
        val transport = FakeTransportAdapter()

        val client = SyncClient.build(this) {
            storage(storage)
            transport(transport)
        }

        var observedId: String? = null
        var observedAllCalled = false
        var upsertedValue: String? = null
        var deletedId: String? = null

        val repo = client.repository<String>(
            observe = { id -> observedId = id; flowOf(null) },
            observeAll = { observedAllCalled = true; flowOf(emptyList()) },
            upsert = { value -> upsertedValue = value },
            delete = { id -> deletedId = id }
        )

        // Test observe delegation
        val result = repo.observe("test-id")
        result.first()
        assertEquals("test-id", observedId)

        // Test observeAll delegation
        repo.observeAll().first()
        assertEquals(true, observedAllCalled)

        // Test upsert delegation
        repo.upsert("test-value")
        assertEquals("test-value", upsertedValue)

        // Test delete delegation
        repo.delete("delete-id")
        assertEquals("delete-id", deletedId)
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
}
