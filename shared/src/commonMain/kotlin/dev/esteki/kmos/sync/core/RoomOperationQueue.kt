package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.storage.SyncDatabase
import dev.esteki.kmos.sync.storage.SyncOperationTable

class RoomOperationQueue(
    private val database: SyncDatabase,
    private val retryPolicy: RetryPolicy,
) : OperationQueue {

    private val dao = database.syncOperationDao()

    override suspend fun enqueue(op: SyncOperation) {
        val existing = dao.getByOperationId(op.operationId)
        if (existing != null) {
            dao.upsert(op.toTable())
        } else {
            dao.upsert(op.toTable())
        }
    }

    override suspend fun dequeuePending(): List<SyncOperation> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun markDone(operationId: String) {
        dao.deleteByOperationId(operationId)
    }

    override suspend fun remove(operationId: String) {
        dao.deleteByOperationId(operationId)
    }

    override suspend fun markFailed(operationId: String, error: SyncError) {
        val existing = dao.getByOperationId(operationId) ?: return
        val op = existing.toDomain()
        val newAttempt = op.attempt + 1
        if (retryPolicy.shouldDeadLetter(newAttempt, error)) {
            dao.deleteByOperationId(operationId)
        } else {
            dao.upsert(op.copy(attempt = newAttempt).toTable())
        }
    }

    override suspend fun size(): Int {
        return dao.count()
    }

    override suspend fun clear() {
        dao.deleteAll()
    }

    private fun SyncOperation.toTable() = SyncOperationTable(
        operationId = operationId,
        entityId = entityId,
        type = type.name,
        attempt = attempt,
        payload = payload,
    )

    private fun SyncOperationTable.toDomain() = SyncOperation(
        operationId = operationId,
        entityId = entityId,
        type = OperationType.valueOf(type),
        attempt = attempt,
        payload = payload,
    )
}
