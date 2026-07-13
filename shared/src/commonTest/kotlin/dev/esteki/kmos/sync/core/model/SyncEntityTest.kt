package dev.esteki.kmos.sync.core.model

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncEntityTest {

    @Test
    fun equalityWithByteArray() {
        val entity1 = SyncEntity(
            id = "id",
            version = 1L,
            updatedAt = Instant.fromEpochMilliseconds(0L),
            deleted = false,
            syncState = SyncState.LocalOnly,
            payload = byteArrayOf(1, 2, 3),
        )
        val entity2 = entity1.copy()

        assertEquals(entity1, entity2)
        assertEquals(entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun copyWithModifiedFields() {
        val entity = SyncEntity(
            id = "id",
            version = 1L,
            updatedAt = Instant.fromEpochMilliseconds(0L),
            deleted = false,
            syncState = SyncState.LocalOnly,
            payload = byteArrayOf(1),
        )

        val updated = entity.copy(
            version = 2L,
            syncState = SyncState.PendingUpload,
        )

        assertEquals("id", updated.id)
        assertEquals(2L, updated.version)
        assertEquals(SyncState.PendingUpload, updated.syncState)
    }
}
