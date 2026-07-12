package dev.esteki.kmos.sync.core

class LastWriteWinsConflictResolver : ConflictResolver<SyncEntityWithTimestamp> {

    override fun resolve(local: SyncEntityWithTimestamp, remote: SyncEntityWithTimestamp): SyncEntityWithTimestamp {
        return if (local.updatedAt >= remote.updatedAt) local else remote
    }
}

interface SyncEntityWithTimestamp {
    val updatedAt: Long
}
