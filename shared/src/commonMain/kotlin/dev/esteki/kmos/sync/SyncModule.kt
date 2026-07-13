package dev.esteki.kmos.sync

import dev.esteki.kmos.sync.core.ConflictResolver
import dev.esteki.kmos.sync.core.ExponentialBackoffRetryPolicy
import dev.esteki.kmos.sync.core.LastWriteWinsConflictResolver
import dev.esteki.kmos.sync.core.RetryPolicy
import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.network.KtorTransportAdapter
import dev.esteki.kmos.sync.storage.RoomStorageAdapter
import dev.esteki.kmos.sync.storage.SyncDatabase
import dev.esteki.kmos.sync.storage.createDatabase
import org.koin.dsl.module

fun syncModule(
    databaseName: String = "sync.db",
) = module {
    single<SyncDatabase> { createDatabase(databaseName) }
    single<StorageAdapter> { RoomStorageAdapter(get()) }
    single<TransportAdapter> { KtorTransportAdapter() }
    single<RetryPolicy> { ExponentialBackoffRetryPolicy() }
    single<ConflictResolver<SyncEntity>> { LastWriteWinsConflictResolver() }
    single<SyncClient> { params ->
        SyncClient.build(params.get()) {
            storage(get())
            transport(get())
            retry(get())
        }
    }
}
