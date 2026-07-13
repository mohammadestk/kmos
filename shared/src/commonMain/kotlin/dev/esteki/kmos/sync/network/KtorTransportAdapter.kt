package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class KtorTransportAdapter(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : TransportAdapter {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun push(op: SyncOperation): PushResult {
        return try {
            val body = ServerObject(
                id = op.entityId,
                name = op.entityId,
                data = Base64.encode(op.payload),
            )

            val response = when (op.type) {
                OperationType.Create -> {
                    httpClient.post("$baseUrl/objects") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }

                OperationType.Update -> {
                    httpClient.put("$baseUrl/objects/${op.entityId}") {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }

                OperationType.Delete -> {
                    httpClient.delete("$baseUrl/objects/${op.entityId}")
                }
            }

            when {
                response.status.value == 409 -> {
                    val remote = fetchEntity(op.entityId)
                    if (remote != null) PushResult.Conflict(remote)
                    else PushResult.Error(SyncError.ConflictDetected)
                }

                response.status.isSuccess() -> {
                    PushResult.Success(
                        version = kotlin.time.Clock.System.now().toEpochMilliseconds()
                    )
                }

                else -> {
                    PushResult.Error(
                        SyncError.ServerError(
                            code = response.status.value,
                            message = response.bodyAsText()
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

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun pull(cursor: String?): PullResult {
        return try {
            val response = httpClient.get("$baseUrl/objects")
            if (response.status.isSuccess()) {
                val objects = response.body<List<ServerObject>>()
                val entities = objects.map {
                    SyncEntity(
                        id = it.id,
                        version = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                        updatedAt = kotlin.time.Clock.System.now(),
                        deleted = false,
                        syncState = dev.esteki.kmos.sync.core.model.SyncState.Synced,
                        payload = Base64.decode(it.data),
                    )
                }
                PullResult(entities = entities, nextCursor = null)
            } else {
                PullResult(entities = emptyList(), nextCursor = null)
            }
        } catch (e: Exception) {
            PullResult(entities = emptyList(), nextCursor = null)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchEntity(entityId: String): SyncEntity? {
        return try {
            val response = httpClient.get("$baseUrl/objects/$entityId")
            if (response.status.isSuccess()) {
                val obj = response.body<ServerObject>()
                SyncEntity(
                    id = obj.id,
                    version = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    updatedAt = kotlin.time.Clock.System.now(),
                    deleted = false,
                    syncState = dev.esteki.kmos.sync.core.model.SyncState.Synced,
                    payload = Base64.decode(obj.data),
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
