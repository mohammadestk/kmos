package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeStorageAdapter : StorageAdapter {
    val entities = mutableMapOf<String, SyncEntity>()
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun read(id: String): SyncEntity? = entities[id]

    override suspend fun write(entity: SyncEntity) {
        entities[entity.id] = entity
        _changes.tryEmit(Unit)
    }

    override suspend fun delete(id: String) {
        entities.remove(id)
        _changes.tryEmit(Unit)
    }

    override suspend fun queryAll(): List<SyncEntity> = entities.values.toList()

    override suspend fun queryPending(): List<SyncEntity> =
        entities.values.filter { it.syncState == SyncState.PendingUpload }

    override suspend fun queryFailed(): List<SyncEntity> =
        entities.values.filter { it.syncState == SyncState.Failed }

    override fun observeChanges(): Flow<Unit> = _changes

    fun clear() {
        entities.clear()
    }

    fun addEntity(entity: SyncEntity) {
        entities[entity.id] = entity
    }

    fun getEntity(id: String): SyncEntity? = entities[id]
}
