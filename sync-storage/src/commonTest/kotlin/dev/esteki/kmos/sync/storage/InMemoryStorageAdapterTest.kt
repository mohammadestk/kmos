package dev.esteki.kmos.sync.storage

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.testing.StorageAdapterContractTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class InMemoryStorageAdapterTest : StorageAdapterContractTest() {
    override fun createAdapter(): StorageAdapter = InMemoryStorageAdapter()

    @Test
    fun testReadReturnsNullForNonExistentEntity() = runTest { readReturnsNullForNonExistentEntity() }

    @Test
    fun testWriteAndRead() = runTest { writeAndRead() }

    @Test
    fun testWriteOverwritesExistingEntity() = runTest { writeOverwritesExistingEntity() }

    @Test
    fun testDeleteRemovesEntity() = runTest { deleteRemovesEntity() }

    @Test
    fun testQueryFailedReturnsOnlyFailedEntities() = runTest { queryFailedReturnsOnlyFailedEntities() }

    @Test
    fun testQueryFailedReturnsEmptyWhenNoFailed() = runTest { queryFailedReturnsEmptyWhenNoFailed() }

    @Test
    fun testDeleteNonExistentIsNoOp() = runTest { deleteNonExistentIsNoOp() }

    @Test
    fun testMultipleWritesSameIdPreservesLatest() = runTest { multipleWritesSameIdPreservesLatest() }

    @Test
    fun testQueryAllReturnsAllEntities() = runTest { queryAllReturnsAllEntities() }

    @Test
    fun testQueryAllReturnsEmptyListWhenNoEntities() = runTest { queryAllReturnsEmptyListWhenNoEntities() }
}
