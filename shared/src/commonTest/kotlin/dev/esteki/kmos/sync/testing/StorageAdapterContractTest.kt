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
