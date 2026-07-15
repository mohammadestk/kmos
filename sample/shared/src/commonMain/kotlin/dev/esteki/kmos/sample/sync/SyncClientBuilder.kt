package dev.esteki.kmos.sample.sync

import dev.esteki.kmos.sync.core.ExponentialBackoffRetryPolicy
import dev.esteki.kmos.sync.core.RetryPolicy
import dev.esteki.kmos.sync.core.StorageAdapter
import dev.esteki.kmos.sync.core.SyncClient
import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.network.KtorTransportAdapter
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration

class SyncClientBuilder {
    private var storageAdapter: StorageAdapter? = null
    private var transportAdapter: TransportAdapter? = null
    private var retryPolicy: RetryPolicy? = null
    private var httpClient: HttpClient? = null
    private var baseUrl: String = "https://api.restful-api.dev"
    private var syncOnForeground: Boolean = false
    private var syncInterval: Duration? = null

    fun storage(adapter: StorageAdapter) = apply { this.storageAdapter = adapter }
    fun transport(adapter: TransportAdapter) = apply { this.transportAdapter = adapter }
    fun retry(policy: RetryPolicy) = apply { this.retryPolicy = policy }
    fun httpClient(client: HttpClient) = apply { this.httpClient = client }
    fun baseUrl(url: String) = apply { this.baseUrl = url }
    fun syncOnForeground(enabled: Boolean) = apply { this.syncOnForeground = enabled }
    fun syncInterval(interval: Duration) = apply { this.syncInterval = interval }

    fun build(scope: CoroutineScope): SyncClient {
        val storage = requireNotNull(storageAdapter) { "StorageAdapter is required" }
        val transport = transportAdapter ?: run {
            val client = httpClient ?: HttpClient()
            KtorTransportAdapter(client, baseUrl)
        }
        val retry = retryPolicy ?: ExponentialBackoffRetryPolicy()

        return SyncClient.build(scope) {
            storage(storage)
            transport(transport)
            retry(retry)
            syncOnForeground(this@SyncClientBuilder.syncOnForeground)
            this@SyncClientBuilder.syncInterval?.let { syncInterval(it) }
        }
    }
}

fun syncClient(scope: CoroutineScope, block: SyncClientBuilder.() -> Unit): SyncClient {
    return SyncClientBuilder().apply(block).build(scope)
}
