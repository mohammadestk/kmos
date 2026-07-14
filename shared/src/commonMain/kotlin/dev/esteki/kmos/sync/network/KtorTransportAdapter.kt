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
import io.ktor.client.request.header
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
import kotlin.time.Clock

class KtorTransportAdapter(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val endpoints: SyncEndpoints = SyncEndpoints(),
) : TransportAdapter {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun push(op: SyncOperation): PushResult {
        return try {
            val body = SyncPushRequest(
                entityId = op.entityId,
                operationType = op.type.name,
                operationId = op.operationId,
                payload = Base64.encode(op.payload),
            )

            val url = "$baseUrl${endpoints.pushUrl(op)}"
            val response = when (op.type) {
                OperationType.Create -> {
                    httpClient.post(url) {
                        contentType(ContentType.Application.Json)
                        header("X-Idempotency-Key", op.operationId)
                        setBody(body)
                    }
                }

                OperationType.Update -> {
                    httpClient.put(url) {
                        contentType(ContentType.Application.Json)
                        header("X-Idempotency-Key", op.operationId)
                        setBody(body)
                    }
                }

                OperationType.Delete -> {
                    httpClient.delete(url) {
                        header("X-Idempotency-Key", op.operationId)
                    }
                }
            }

            when {
                response.status.value == 409 -> {
                    val remote = fetchEntity(op.entityId)
                    if (remote != null) PushResult.Conflict(remote)
                    else PushResult.Error(SyncError.ConflictDetected)
                }

                response.status.isSuccess() -> {
                    val pushResponse = try {
                        response.body<SyncPushResponse>()
                    } catch (e: Exception) {
                        null
                    }
                    PushResult.Success(
                        version = pushResponse?.version ?: Clock.System.now().toEpochMilliseconds()
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
            val url = "$baseUrl${endpoints.pullUrl(cursor)}"
            val response = httpClient.get(url)
            if (response.status.isSuccess()) {
                val pullResponse = response.body<SyncPullResponse>()
                val entities = pullResponse.entities.map { it.toSyncEntity() }
                PullResult(
                    entities = entities,
                    nextCursor = pullResponse.nextCursor,
                )
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
            val url = "$baseUrl${endpoints.singleEntityUrl(entityId)}"
            val response = httpClient.get(url)
            if (response.status.isSuccess()) {
                val obj = response.body<SyncEntityResponse>()
                obj.toSyncEntity()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
