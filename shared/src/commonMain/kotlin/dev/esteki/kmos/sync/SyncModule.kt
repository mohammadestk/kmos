package dev.esteki.kmos.sync

import dev.esteki.kmos.sync.core.ExponentialBackoffRetryPolicy
import dev.esteki.kmos.sync.core.RetryPolicy
import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.network.KtorTransportAdapter
import org.koin.dsl.module

val syncModule = module {
    single<RetryPolicy> { ExponentialBackoffRetryPolicy() }
    single<TransportAdapter> { KtorTransportAdapter() }
}
