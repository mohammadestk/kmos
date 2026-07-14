package dev.esteki.kmos.sync.storage

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryStorageAdapter : StorageAdapter {
    private val mutex = Mutex()
    private val entities = mutableMapOf<String, SyncEntity>()

    override suspend fun read(id: String): SyncEntity? = mutex.withLock {
        entities[id]
    }

    override suspend fun write(entity: SyncEntity) {
        mutex.withLock {
            entities[entity.id] = entity
        }
    }

    override suspend fun delete(id: String) {
        mutex.withLock {
            entities.remove(id)
        }
    }

    override suspend fun queryPending(): List<SyncEntity> = mutex.withLock {
        entities.values.filter { it.syncState == SyncState.PendingUpload }
    }

    override suspend fun queryFailed(): List<SyncEntity> = mutex.withLock {
        entities.values.filter { it.syncState == SyncState.Failed }
    }

    fun clear() {
        entities.clear()
    }
}
