package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationQueueTest {

    private val retryPolicy = ExponentialBackoffRetryPolicy(
        maxAttempts = 3,
    )

    private val queue = InMemoryOperationQueue(retryPolicy)

    @Test
    fun enqueueAndDequeue() = runTest {
        val op = createOperation("op-1")
        queue.enqueue(op)

        val pending = queue.dequeuePending()
        assertEquals(1, pending.size)
        assertEquals("op-1", pending[0].operationId)
    }

    @Test
    fun idempotencyDeduplication() = runTest {
        val op1 = createOperation("op-1", attempt = 0)
        val op2 = createOperation("op-1", attempt = 1)

        queue.enqueue(op1)
        queue.enqueue(op2)

        val pending = queue.dequeuePending()
        assertEquals(1, pending.size)
        assertEquals(1, pending[0].attempt)
    }

    @Test
    fun markDoneRemovesFromQueue() = runTest {
        val op = createOperation("op-1")
        queue.enqueue(op)
        assertEquals(1, queue.size())

        queue.markDone("op-1")
        assertEquals(0, queue.size())
    }

    @Test
    fun markFailedIncrementsAttempt() = runTest {
        val op = createOperation("op-1", attempt = 0)
        queue.enqueue(op)

        queue.markFailed("op-1")

        val pending = queue.dequeuePending()
        assertEquals(1, pending.size)
        assertEquals(1, pending[0].attempt)
    }

    @Test
    fun markFailedDeadLettersAfterMaxAttempts() = runTest {
        val op = createOperation("op-1", attempt = 2)
        queue.enqueue(op)

        queue.markFailed("op-1")

        assertEquals(0, queue.size())
    }

    @Test
    fun clearRemovesAllOperations() = runTest {
        queue.enqueue(createOperation("op-1"))
        queue.enqueue(createOperation("op-2"))
        assertEquals(2, queue.size())

        queue.clear()
        assertEquals(0, queue.size())
    }

    private fun createOperation(
        operationId: String,
        attempt: Int = 0,
    ) = SyncOperation(
        operationId = operationId,
        entityId = "entity-$operationId",
        type = OperationType.Create,
        attempt = attempt,
        payload = byteArrayOf(),
    )
}
