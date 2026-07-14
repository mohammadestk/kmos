package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

abstract class StorageAdapterContractTest {

    protected abstract fun createAdapter(): StorageAdapter

    @Test
    fun readReturnsNullForNonExistentEntity() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        val result = adapter.read("non-existent")
        assertNull(result)
    }

    @Test
    fun writeAndRead() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        val entity = createEntity("id-1", version = 1L, syncState = SyncState.LocalOnly)

        adapter.write(entity)
        val result = adapter.read("id-1")

        assertEquals(entity, result)
    }

    @Test
    fun writeOverwritesExistingEntity() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        val entity1 = createEntity("id-1", version = 1L)
        val entity2 = createEntity("id-1", version = 2L)

        adapter.write(entity1)
        adapter.write(entity2)
        val result = adapter.read("id-1")

        assertEquals(2L, result?.version)
    }

    @Test
    fun queryPendingReturnsOnlyPendingEntities() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        adapter.write(createEntity("id-1", syncState = SyncState.LocalOnly))
        adapter.write(createEntity("id-2", syncState = SyncState.PendingUpload))
        adapter.write(createEntity("id-3", syncState = SyncState.Synced))
        adapter.write(createEntity("id-4", syncState = SyncState.PendingUpload))

        val pending = adapter.queryPending()

        assertEquals(2, pending.size)
        assertEquals(true, pending.all { it.syncState == SyncState.PendingUpload })
    }

    @Test
    fun queryPendingReturnsEmptyListWhenNoPending() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        adapter.write(createEntity("id-1", syncState = SyncState.Synced))

        val pending = adapter.queryPending()

        assertEquals(0, pending.size)
    }

    @Test
    fun deleteRemovesEntity() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        val entity = createEntity("id-1", syncState = SyncState.LocalOnly)
        adapter.write(entity)

        adapter.delete("id-1")
        val result = adapter.read("id-1")

        assertNull(result)
    }

    @Test
    fun queryFailedReturnsOnlyFailedEntities() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        adapter.write(createEntity("id-1", syncState = SyncState.LocalOnly))
        adapter.write(createEntity("id-2", syncState = SyncState.Failed))
        adapter.write(createEntity("id-3", syncState = SyncState.Synced))
        adapter.write(createEntity("id-4", syncState = SyncState.Failed))

        val failed = adapter.queryFailed()

        assertEquals(2, failed.size)
        assertEquals(true, failed.all { it.syncState == SyncState.Failed })
    }

    @Test
    fun queryFailedReturnsEmptyWhenNoFailed() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        adapter.write(createEntity("id-1", syncState = SyncState.Synced))

        val failed = adapter.queryFailed()

        assertEquals(0, failed.size)
    }

    @Test
    fun deleteNonExistentIsNoOp() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()

        // Should not throw
        adapter.delete("non-existent")

        assertNull(adapter.read("non-existent"))
    }

    @Test
    fun multipleWritesSameIdPreservesLatest() = kotlinx.coroutines.test.runTest {
        val adapter = createAdapter()
        val entity1 = createEntity("id-1", version = 1L, syncState = SyncState.LocalOnly)
        val entity2 = createEntity("id-1", version = 2L, syncState = SyncState.PendingUpload)
        val entity3 = createEntity("id-1", version = 3L, syncState = SyncState.Synced)

        adapter.write(entity1)
        adapter.write(entity2)
        adapter.write(entity3)

        val result = adapter.read("id-1")
        kotlin.test.assertNotNull(result)
        assertEquals(3L, result.version)
        assertEquals(SyncState.Synced, result.syncState)
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
