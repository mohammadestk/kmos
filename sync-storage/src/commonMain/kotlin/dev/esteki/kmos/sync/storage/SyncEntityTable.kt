package dev.esteki.kmos.sync.storage

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sync_entities")
internal data class SyncEntityTable(
    @PrimaryKey val id: String,
    val version: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val syncState: String,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SyncEntityTable) return false
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
