package dev.esteki.kmos.sync.storage

import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.testing.StorageAdapterContractTest

class InMemoryStorageAdapterTest : StorageAdapterContractTest() {
    override fun createAdapter(): StorageAdapter = InMemoryStorageAdapter()
}
