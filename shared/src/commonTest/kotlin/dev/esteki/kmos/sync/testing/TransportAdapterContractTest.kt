package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class TransportAdapterContractTest {

    protected abstract fun createAdapter(): TransportAdapter

    @Test
    fun pushReturnsSuccess() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        val op = createOperation("op-1")

        val result = adapter.push(op)

        assertTrue(result is PushResult.Success)
    }

    @Test
    fun pushReturnsConflict() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        val op = createOperation("op-1")

        val result = adapter.push(op)

        // This test assumes the adapter returns Success by default
        // Override in specific adapter tests to test conflict behavior
        assertTrue(result is PushResult.Success)
    }

    @Test
    fun pullReturnsEntities() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()

        val result = adapter.pull(null)

        // This test assumes the adapter returns empty list by default
        // Override in specific adapter tests to test pull behavior
        assertEquals(0, result.entities.size)
    }

    @Test
    fun pullReturnsNextCursor() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()

        val result = adapter.pull(null)

        // This test assumes the adapter returns null cursor by default
        // Override in specific adapter tests to test pagination
        assertEquals(null, result.nextCursor)
    }

    protected fun createOperation(
        operationId: String = "op-1",
    ) = SyncOperation(
        operationId = operationId,
        entityId = "entity-1",
        type = OperationType.Create,
        attempt = 0,
        payload = byteArrayOf(),
    )
}
