package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OperationQueue(
    private val retryPolicy: RetryPolicy,
) {
    private val mutex = Mutex()
    private val operations = mutableListOf<SyncOperation>()

    suspend fun enqueue(op: SyncOperation) = mutex.withLock {
        val existingIndex = operations.indexOfFirst { it.operationId == op.operationId }
        if (existingIndex >= 0) {
            operations[existingIndex] = op
        } else {
            operations.add(op)
        }
    }

    /**
     * Returns a snapshot of all operations in the queue without removing them.
     * This is a non-destructive peek operation.
     */
    suspend fun dequeuePending(): List<SyncOperation> = mutex.withLock {
        operations.toList()
    }

    suspend fun markDone(operationId: String) = mutex.withLock {
        operations.removeAll { it.operationId == operationId }
    }

    suspend fun remove(operationId: String) = mutex.withLock {
        operations.removeAll { it.operationId == operationId }
    }

    suspend fun markFailed(operationId: String, error: SyncError = SyncError.NetworkTimeout) = mutex.withLock {
        val index = operations.indexOfFirst { it.operationId == operationId }
        if (index >= 0) {
            val op = operations[index]
            val newAttempt = op.attempt + 1
            // Dead-letter if retry policy says so (e.g., max attempts reached)
            if (retryPolicy.shouldDeadLetter(newAttempt, error)) {
                operations.removeAt(index)
            } else {
                operations[index] = op.copy(attempt = newAttempt)
            }
        }
    }

    suspend fun size(): Int = mutex.withLock {
        operations.size
    }

    suspend fun clear() = mutex.withLock {
        operations.clear()
    }
}
