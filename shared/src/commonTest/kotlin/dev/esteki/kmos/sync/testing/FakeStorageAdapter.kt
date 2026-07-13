package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState

class FakeStorageAdapter : StorageAdapter {
    val entities = mutableMapOf<String, SyncEntity>()

    override suspend fun read(id: String): SyncEntity? = entities[id]

    override suspend fun write(entity: SyncEntity) {
        entities[entity.id] = entity
    }

    override suspend fun delete(id: String) {
        entities.remove(id)
    }

    override suspend fun queryPending(): List<SyncEntity> =
        entities.values.filter { it.syncState == SyncState.PendingUpload }

    fun clear() {
        entities.clear()
    }

    fun addEntity(entity: SyncEntity) {
        entities[entity.id] = entity
    }

    fun getEntity(id: String): SyncEntity? = entities[id]
}
