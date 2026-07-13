package dev.esteki.kmos.sync

import dev.esteki.kmos.sync.core.ConflictResolver
import dev.esteki.kmos.sync.core.RetryPolicy
import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class SyncModuleTest {

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun moduleCreatesSuccessfully() {
        val app = koinApplication {
            modules(syncModule())
        }

        val koin = app.koin

        assertNotNull(koin.get<RetryPolicy>())
        assertNotNull(koin.get<TransportAdapter>())
        assertNotNull(koin.get<StorageAdapter>())
        assertNotNull(koin.get<ConflictResolver<SyncEntity>>())
    }
}
