package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncOperation
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.isSuccess
import kotlinx.io.IOException

class KtorTransportAdapter(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val protocol: SyncApiProtocol,
) : TransportAdapter {

    override suspend fun push(op: SyncOperation): PushResult {
        return try {
            val url = "$baseUrl${protocol.pushUrl(op)}"
            val requestConfig = protocol.buildPushRequest(op)

            val response = when (op.type) {
                OperationType.Create -> httpClient.post(url, requestConfig)
                OperationType.Update -> httpClient.put(url, requestConfig)
                OperationType.Delete -> httpClient.delete(url, requestConfig)
            }

            if (response.status.isSuccess()) {
                protocol.parsePushResponse(response)
            } else {
                protocol.parsePushResponse(response)
            }
        } catch (e: Exception) {
            when (e) {
                is IOException -> PushResult.Error(dev.esteki.kmos.sync.core.model.SyncError.NetworkTimeout)
                is kotlinx.serialization.SerializationException -> PushResult.Error(dev.esteki.kmos.sync.core.model.SyncError.SerializationError)
                else -> PushResult.Error(dev.esteki.kmos.sync.core.model.SyncError.Unknown(e))
            }
        }
    }

    override suspend fun pull(cursor: String?): PullResult {
        return try {
            val url = "$baseUrl${protocol.pullUrl(cursor)}"
            val response = httpClient.get(url)
            protocol.parsePullResponse(response)
        } catch (e: Exception) {
            PullResult(entities = emptyList(), nextCursor = null)
        }
    }
}
