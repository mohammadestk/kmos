package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncEntity
import kotlinx.coroutines.flow.Flow

interface StorageAdapter {
    suspend fun read(id: String): SyncEntity?
    suspend fun write(entity: SyncEntity)
    suspend fun delete(id: String)
    suspend fun queryAll(): List<SyncEntity>
    suspend fun queryFailed(): List<SyncEntity>
    fun observeChanges(): Flow<Unit>
}
