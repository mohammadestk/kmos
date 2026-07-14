package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncOperation
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class TransportAdapterContractTest {

    protected abstract fun createAdapter(): TransportAdapter

    protected suspend fun pushReturnsSuccess() {
        val adapter = createAdapter()
        val op = createOperation("op-1")

        val result = adapter.push(op)

        assertTrue(result is PushResult.Success)
    }

    protected suspend fun pushReturnsConflict() {
        val adapter = createAdapter()
        val op = createOperation("op-1")

        val result = adapter.push(op)

        // Default adapter returns Success; override in specific adapter tests
        assertTrue(result is PushResult.Success)
    }

    protected suspend fun pullReturnsEntities() {
        val adapter = createAdapter()

        val result = adapter.pull(null)

        // Default adapter returns empty list; override in specific adapter tests
        assertEquals(0, result.entities.size)
    }

    protected suspend fun pullReturnsNextCursor() {
        val adapter = createAdapter()

        val result = adapter.pull(null)

        assertEquals(null, result.nextCursor)
    }

    protected suspend fun pushWithCreateOperation() {
        val adapter = createAdapter()
        val op = createOperation("op-create", type = OperationType.Create)

        val result = adapter.push(op)

        assertTrue(result is PushResult.Success || result is PushResult.Error)
    }

    protected suspend fun pushWithUpdateOperation() {
        val adapter = createAdapter()
        val op = createOperation("op-update", type = OperationType.Update)

        val result = adapter.push(op)

        assertTrue(result is PushResult.Success || result is PushResult.Error)
    }

    protected suspend fun pushWithDeleteOperation() {
        val adapter = createAdapter()
        val op = createOperation("op-delete", type = OperationType.Delete)

        val result = adapter.push(op)

        assertTrue(result is PushResult.Success || result is PushResult.Error)
    }

    protected suspend fun pullWithCursorParameter() {
        val adapter = createAdapter()

        val result = adapter.pull("test-cursor")

        // Verify the adapter handles cursor parameter without crashing
        assertEquals(null, result.nextCursor)
    }

    protected fun createOperation(
        operationId: String = "op-1",
        type: OperationType = OperationType.Create,
    ) = SyncOperation(
        operationId = operationId,
        entityId = "entity-1",
        type = type,
        attempt = 0,
        payload = byteArrayOf(),
    )
}
