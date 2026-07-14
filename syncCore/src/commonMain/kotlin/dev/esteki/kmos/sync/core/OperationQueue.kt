package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation

interface OperationQueue {
    suspend fun enqueue(op: SyncOperation)
    suspend fun dequeuePending(): List<SyncOperation>
    suspend fun markDone(operationId: String)
    suspend fun remove(operationId: String)
    suspend fun markFailed(operationId: String, error: SyncError = SyncError.NetworkTimeout)
    suspend fun size(): Int
    suspend fun clear()
}
