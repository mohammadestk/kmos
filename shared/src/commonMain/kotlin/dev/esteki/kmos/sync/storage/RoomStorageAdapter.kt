package dev.esteki.kmos.sync.storage

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.datetime.Instant

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

    override suspend fun queryPending(): List<SyncEntity> {
        return dao.queryPending().map { it.toDomain() }
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
