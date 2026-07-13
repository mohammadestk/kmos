package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncEntity

interface StorageAdapter {
    suspend fun read(id: String): SyncEntity?
    suspend fun write(entity: SyncEntity)
    suspend fun delete(id: String)
    suspend fun queryPending(): List<SyncEntity>
}
