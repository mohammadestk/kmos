package dev.esteki.kmos.sync

import dev.esteki.kmos.sync.core.ExponentialBackoffRetryPolicy
import dev.esteki.kmos.sync.core.RetryPolicy
import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.network.KtorTransportAdapter
import kotlinx.coroutines.CoroutineScope

class SyncClientBuilder {
    private var storageAdapter: StorageAdapter? = null
    private var transportAdapter: TransportAdapter? = null
    private var retryPolicy: RetryPolicy? = null

    fun storage(adapter: StorageAdapter) = apply { this.storageAdapter = adapter }
    fun transport(adapter: TransportAdapter) = apply { this.transportAdapter = adapter }
    fun retry(policy: RetryPolicy) = apply { this.retryPolicy = policy }

    fun build(scope: CoroutineScope): SyncClient {
        val storage = requireNotNull(storageAdapter) { "StorageAdapter is required" }
        val transport = transportAdapter ?: KtorTransportAdapter()
        val retry = retryPolicy ?: ExponentialBackoffRetryPolicy()

        return SyncClient.build(scope) {
            storage(storage)
            transport(transport)
            retry(retry)
        }
    }
}

fun syncClient(scope: CoroutineScope, block: SyncClientBuilder.() -> Unit): SyncClient {
    return SyncClientBuilder().apply(block).build(scope)
}
