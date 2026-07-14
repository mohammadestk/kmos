package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlin.time.Instant

abstract class StorageAdapterContractTest {

    protected abstract fun createAdapter(): StorageAdapter

    protected suspend fun readReturnsNullForNonExistentEntity() {
        val adapter = createAdapter()
        val result = adapter.read("non-existent")
        kotlin.test.assertNull(result)
    }

    protected suspend fun writeAndRead() {
        val adapter = createAdapter()
        val entity = createEntity("id-1", version = 1L, syncState = SyncState.LocalOnly)

        adapter.write(entity)
        val result = adapter.read("id-1")

        kotlin.test.assertEquals(entity, result)
    }

    protected suspend fun writeOverwritesExistingEntity() {
        val adapter = createAdapter()
        val entity1 = createEntity("id-1", version = 1L)
        val entity2 = createEntity("id-1", version = 2L)

        adapter.write(entity1)
        adapter.write(entity2)
        val result = adapter.read("id-1")

        kotlin.test.assertEquals(2L, result?.version)
    }

    protected suspend fun queryPendingReturnsOnlyPendingEntities() {
        val adapter = createAdapter()
        adapter.write(createEntity("id-1", syncState = SyncState.LocalOnly))
        adapter.write(createEntity("id-2", syncState = SyncState.PendingUpload))
        adapter.write(createEntity("id-3", syncState = SyncState.Synced))
        adapter.write(createEntity("id-4", syncState = SyncState.PendingUpload))

        val pending = adapter.queryPending()

        kotlin.test.assertEquals(2, pending.size)
        kotlin.test.assertEquals(true, pending.all { it.syncState == SyncState.PendingUpload })
    }

    protected suspend fun queryPendingReturnsEmptyListWhenNoPending() {
        val adapter = createAdapter()
        adapter.write(createEntity("id-1", syncState = SyncState.Synced))

        val pending = adapter.queryPending()

        kotlin.test.assertEquals(0, pending.size)
    }

    protected suspend fun deleteRemovesEntity() {
        val adapter = createAdapter()
        val entity = createEntity("id-1", syncState = SyncState.LocalOnly)
        adapter.write(entity)

        adapter.delete("id-1")
        val result = adapter.read("id-1")

        kotlin.test.assertNull(result)
    }

    protected suspend fun queryFailedReturnsOnlyFailedEntities() {
        val adapter = createAdapter()
        adapter.write(createEntity("id-1", syncState = SyncState.LocalOnly))
        adapter.write(createEntity("id-2", syncState = SyncState.Failed))
        adapter.write(createEntity("id-3", syncState = SyncState.Synced))
        adapter.write(createEntity("id-4", syncState = SyncState.Failed))

        val failed = adapter.queryFailed()

        kotlin.test.assertEquals(2, failed.size)
        kotlin.test.assertEquals(true, failed.all { it.syncState == SyncState.Failed })
    }

    protected suspend fun queryFailedReturnsEmptyWhenNoFailed() {
        val adapter = createAdapter()
        adapter.write(createEntity("id-1", syncState = SyncState.Synced))

        val failed = adapter.queryFailed()

        kotlin.test.assertEquals(0, failed.size)
    }

    protected suspend fun deleteNonExistentIsNoOp() {
        val adapter = createAdapter()

        // Should not throw
        adapter.delete("non-existent")

        kotlin.test.assertNull(adapter.read("non-existent"))
    }

    protected suspend fun multipleWritesSameIdPreservesLatest() {
        val adapter = createAdapter()
        val entity1 = createEntity("id-1", version = 1L, syncState = SyncState.LocalOnly)
        val entity2 = createEntity("id-1", version = 2L, syncState = SyncState.PendingUpload)
        val entity3 = createEntity("id-1", version = 3L, syncState = SyncState.Synced)

        adapter.write(entity1)
        adapter.write(entity2)
        adapter.write(entity3)

        val result = adapter.read("id-1")
        kotlin.test.assertNotNull(result)
        kotlin.test.assertEquals(3L, result.version)
        kotlin.test.assertEquals(SyncState.Synced, result.syncState)
    }

    protected fun createEntity(
        id: String,
        version: Long = 1L,
        syncState: SyncState = SyncState.LocalOnly,
    ) = SyncEntity(
        id = id,
        version = version,
        updatedAt = Instant.fromEpochMilliseconds(0L),
        deleted = false,
        syncState = syncState,
        payload = byteArrayOf(),
    )
}
