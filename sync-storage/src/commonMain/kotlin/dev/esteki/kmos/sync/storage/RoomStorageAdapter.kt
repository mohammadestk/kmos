package dev.esteki.kmos.sync.storage

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class RoomStorageAdapter(
    private val database: SyncDatabase,
) : StorageAdapter {

    private val dao = database.syncEntityDao()

    override suspend fun read(id: String): SyncEntity? {
        val table = dao.getById(id) ?: return null
        return table.toDomain()
    }

    override suspend fun write(entity: SyncEntity) {
        dao.upsert(entity.toTable())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun queryAll(): List<SyncEntity> {
        return dao.queryAll().map { it.toDomain() }
    }

    override suspend fun queryPending(): List<SyncEntity> {
        return dao.queryPending().map { it.toDomain() }
    }

    override suspend fun queryFailed(): List<SyncEntity> {
        return dao.queryFailed().map { it.toDomain() }
    }

    override fun observeChanges(): Flow<Unit> {
        return dao.observeChangeCount().map { }
    }

    private fun SyncEntityTable.toDomain() = SyncEntity(
        id = id,
        version = version,
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        deleted = deleted,
        syncState = SyncState.valueOf(syncState),
        payload = payload,
    )

    private fun SyncEntity.toTable() = SyncEntityTable(
        id = id,
        version = version,
        updatedAt = updatedAt.toEpochMilliseconds(),
        deleted = deleted,
        syncState = syncState.name,
        payload = payload,
    )
}
