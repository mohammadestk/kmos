package dev.esteki.kmos.sync.core.model

data class SyncOperation(
    val operationId: String,
    val entityId: String,
    val type: OperationType,
    val attempt: Int,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SyncOperation) return false
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
        result = 31 * result + attempt
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
