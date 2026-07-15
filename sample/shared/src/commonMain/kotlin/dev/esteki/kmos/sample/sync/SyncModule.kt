package dev.esteki.kmos.sample.sync

import dev.esteki.kmos.sample.model.TodoRepository
import dev.esteki.kmos.sample.viewmodel.TodoViewModel
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
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val appModule = module {
    val databaseName = "sync.db"
    val httpClient = HttpClient()
    val baseUrl = "https://api.restful-api.dev"

    single<SyncDatabase> { createDatabase(databaseName) }
    single<StorageAdapter> { RoomStorageAdapter(get()) }
    single<TransportAdapter> { KtorTransportAdapter(httpClient, baseUrl) }
    single<RetryPolicy> { ExponentialBackoffRetryPolicy() }
    single<ConflictResolver<SyncEntity>> { LastWriteWinsConflictResolver() }
    single<SyncClient> {
        val dispatcher = CoroutineScope(Dispatchers.Default)

        SyncClient.build(dispatcher) {
            storage(get())
            transport(get())
            retry(get())
        }
    }
    single { TodoRepository(get()) }

    viewModel<TodoViewModel>()
}
