package dev.esteki.kmos.sync.core

import kotlinx.coroutines.flow.Flow

interface SyncRepository<T> {
    fun observe(id: String): Flow<T?>
    fun observeAll(): Flow<List<T>>
    suspend fun upsert(value: T)
    suspend fun delete(id: String)
}
