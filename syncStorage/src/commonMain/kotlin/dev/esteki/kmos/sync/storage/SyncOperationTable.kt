package dev.esteki.kmos.sync.storage

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sync_operations")
data class SyncOperationTable(
    @PrimaryKey val operationId: String,
    val entityId: String,
    val type: String,
    val attempt: Int,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SyncOperationTable) return false
        return operationId == other.operationId &&
            entityId == other.entityId &&
            type == other.type &&
            attempt == other.attempt &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = operationId.hashCode()
        result = 31 * result + entityId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + attempt.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
