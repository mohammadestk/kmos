package dev.esteki.kmos.sync.storage

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.testing.StorageAdapterContractTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

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
}
