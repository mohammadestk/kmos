package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock

class KtorTransportAdapter(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val endpoints: SyncEndpoints = SyncEndpoints(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TransportAdapter {

    override suspend fun push(op: SyncOperation): PushResult {
        return try {
            val data = payloadToJsonElement(op.payload)
            val url = "$baseUrl${endpoints.pushUrl(op)}"

            val response = when (op.type) {
                OperationType.Create -> {
                    httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        setBody(CreateObjectRequest(name = op.entityId, data = data))
                    }
                }

                OperationType.Update -> {
                    httpClient.put(url) {
                        contentType(ContentType.Application.Json)
                        setBody(UpdateObjectRequest(name = op.entityId, data = data))
                    }
                }

                OperationType.Delete -> {
                    httpClient.delete(url)
                }
            }

            when {
                response.status.isSuccess() -> {
                    PushResult.Success(version = Clock.System.now().toEpochMilliseconds())
                }

                response.status.value == 404 -> {
                    PushResult.Error(SyncError.ServerError(404, response.bodyAsText()))
                }

                else -> {
                    PushResult.Error(
                        SyncError.ServerError(
                            code = response.status.value,
                            message = response.bodyAsText(),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            when (e) {
                is IOException -> PushResult.Error(SyncError.NetworkTimeout)
                is kotlinx.serialization.SerializationException -> PushResult.Error(SyncError.SerializationError)
                else -> PushResult.Error(SyncError.Unknown(e))
            }
        }
    }

    override suspend fun pull(cursor: String?): PullResult {
        return try {
            val url = "$baseUrl${endpoints.pullUrl(cursor)}"
            val response = httpClient.get(url)
            if (response.status.isSuccess()) {
                val objects = response.body<List<RestObject>>()
                val entities = objects.map { it.toSyncEntity() }
                PullResult(entities = entities, nextCursor = null)
            } else {
                PullResult(entities = emptyList(), nextCursor = null)
            }
        } catch (e: Exception) {
            PullResult(entities = emptyList(), nextCursor = null)
        }
    }

    private fun payloadToJsonElement(payload: ByteArray): JsonElement? {
        if (payload.isEmpty()) return null
        return try {
            json.parseToJsonElement(payload.decodeToString())
        } catch (e: Exception) {
            null
        }
    }
}
