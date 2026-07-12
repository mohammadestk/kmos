package dev.esteki.kmos.sync.core.model

data class SyncEntity(
    val id: String,
    val version: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val syncState: SyncState,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SyncEntity) return false
        return id == other.id &&
            version == other.version &&
            updatedAt == other.updatedAt &&
            deleted == other.deleted &&
            syncState == other.syncState &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + deleted.hashCode()
        result = 31 * result + syncState.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
