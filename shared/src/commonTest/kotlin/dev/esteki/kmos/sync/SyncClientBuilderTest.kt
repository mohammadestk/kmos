package dev.esteki.kmos.sync

import dev.esteki.kmos.sync.core.ExponentialBackoffRetryPolicy
import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.testing.FakeStorageAdapter
import dev.esteki.kmos.sync.testing.FakeTransportAdapter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class SyncClientBuilderTest {

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
}
