package dev.esteki.kmos.sync.core

import kotlinx.coroutines.flow.Flow

interface SyncRepository<T> {
    suspend fun read(id: String): T?
    suspend fun readAll(): List<T>
    suspend fun upsert(value: T)
    suspend fun delete(id: String)
    fun observeAll(): Flow<List<T>>
}
