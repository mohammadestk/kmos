package dev.esteki.kmos.sync.core

import kotlin.test.Test
import kotlin.test.assertEquals

class LastWriteWinsConflictResolverTest {

    private val resolver = LastWriteWinsConflictResolver()

    @Test
    fun newerLocalWins() {
        val local = TestEntity(updatedAt = 2000L)
        val remote = TestEntity(updatedAt = 1000L)

        val resolved = resolver.resolve(local, remote)

        assertEquals(2000L, resolved.updatedAt)
    }

    @Test
    fun newerRemoteWins() {
        val local = TestEntity(updatedAt = 1000L)
        val remote = TestEntity(updatedAt = 2000L)

        val resolved = resolver.resolve(local, remote)

        assertEquals(2000L, resolved.updatedAt)
    }

    @Test
    fun equalTimestampsLocalWins() {
        val local = TestEntity(updatedAt = 1000L, id = "local")
        val remote = TestEntity(updatedAt = 1000L, id = "remote")

        val resolved = resolver.resolve(local, remote) as TestEntity

        assertEquals(1000L, resolved.updatedAt)
        assertEquals("local", resolved.id)
    }

    private data class TestEntity(
        override val updatedAt: Long,
        val id: String = "local",
    ) : SyncEntityWithTimestamp
}
