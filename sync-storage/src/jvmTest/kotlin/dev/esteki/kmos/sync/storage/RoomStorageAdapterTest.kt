package dev.esteki.kmos.sync.storage

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.testing.StorageAdapterContractTest
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore("BundledSQLiteDriver native library cannot load in JVM tests")
class RoomStorageAdapterTest : StorageAdapterContractTest() {

    private lateinit var database: SyncDatabase
    private lateinit var adapter: RoomStorageAdapter

    @BeforeTest
    fun setup() {
        database = createInMemoryDatabase()
        adapter = RoomStorageAdapter(database)
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    override fun createAdapter(): StorageAdapter = adapter

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
