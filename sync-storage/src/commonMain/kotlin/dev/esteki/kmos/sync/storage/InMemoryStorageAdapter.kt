package dev.esteki.kmos.sync.storage

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryStorageAdapter : StorageAdapter {
    private val mutex = Mutex()
    private val entities = mutableMapOf<String, SyncEntity>()
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun read(id: String): SyncEntity? = mutex.withLock {
        entities[id]
    }

    override suspend fun write(entity: SyncEntity) {
        mutex.withLock {
            entities[entity.id] = entity
        }
        _changes.tryEmit(Unit)
    }

    override suspend fun delete(id: String) {
        mutex.withLock {
            entities.remove(id)
        }
        _changes.tryEmit(Unit)
    }

    override suspend fun queryAll(): List<SyncEntity> = mutex.withLock {
        entities.values.toList()
    }

    override suspend fun queryFailed(): List<SyncEntity> = mutex.withLock {
        entities.values.filter { it.syncState == SyncState.Failed }
    }

    override fun observeChanges(): Flow<Unit> = _changes

    fun clear() {
        entities.clear()
    }
}
