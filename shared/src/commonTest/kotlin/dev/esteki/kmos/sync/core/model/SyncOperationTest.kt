package dev.esteki.kmos.sync.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncOperationTest {

    @Test
    fun operationIdIsRequired() {
        val operation = SyncOperation(
            operationId = "unique-key",
            entityId = "entity-id",
            type = OperationType.Update,
            attempt = 1,
            payload = byteArrayOf(),
        )

        assertEquals("unique-key", operation.operationId)
    }

    @Test
    fun equalityWithByteArray() {
        val op1 = SyncOperation(
            operationId = "op-1",
            entityId = "entity-1",
            type = OperationType.Create,
            attempt = 0,
            payload = byteArrayOf(10, 20, 30),
        )
        val op2 = op1.copy()

        assertEquals(op1, op2)
        assertEquals(op1.hashCode(), op2.hashCode())
    }
}
