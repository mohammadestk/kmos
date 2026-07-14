package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class LastWriteWinsConflictResolverTest {

    private val resolver = LastWriteWinsConflictResolver()

    @Test
    fun newerLocalWins() {
        val local = createEntity("local", updatedAt = Instant.fromEpochMilliseconds(2000L))
        val remote = createEntity("remote", updatedAt = Instant.fromEpochMilliseconds(1000L))

        val resolved = resolver.resolve(local, remote)

        assertEquals(Instant.fromEpochMilliseconds(2000L), resolved.updatedAt)
    }

    @Test
    fun newerRemoteWins() {
        val local = createEntity("local", updatedAt = Instant.fromEpochMilliseconds(1000L))
        val remote = createEntity("remote", updatedAt = Instant.fromEpochMilliseconds(2000L))

        val resolved = resolver.resolve(local, remote)

        assertEquals(Instant.fromEpochMilliseconds(2000L), resolved.updatedAt)
    }

    @Test
    fun equalTimestampsLocalWins() {
        val local = createEntity("local", updatedAt = Instant.fromEpochMilliseconds(1000L))
        val remote = createEntity("remote", updatedAt = Instant.fromEpochMilliseconds(1000L))

        val resolved = resolver.resolve(local, remote)

        assertEquals(Instant.fromEpochMilliseconds(1000L), resolved.updatedAt)
        assertEquals("local", resolved.id)
    }

    private fun createEntity(
        id: String,
        updatedAt: Instant,
    ) = SyncEntity(
        id = id,
        version = 1L,
        updatedAt = updatedAt,
        deleted = false,
        syncState = SyncState.LocalOnly,
        payload = byteArrayOf(),
    )
}
