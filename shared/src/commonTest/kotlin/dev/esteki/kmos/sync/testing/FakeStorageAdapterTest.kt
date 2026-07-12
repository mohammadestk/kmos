package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.StorageAdapter

class FakeStorageAdapterTest : StorageAdapterContractTest() {
    override fun createAdapter(): StorageAdapter = FakeStorageAdapter()
}
