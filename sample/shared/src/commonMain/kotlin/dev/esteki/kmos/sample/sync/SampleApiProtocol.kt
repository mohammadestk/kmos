package dev.esteki.kmos.sample.sync

import dev.esteki.kmos.sync.core.model.OperationType
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncError
import dev.esteki.kmos.sync.core.model.SyncOperation
import dev.esteki.kmos.sync.core.model.SyncState
import dev.esteki.kmos.sync.network.SyncApiProtocol
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock

class SampleApiProtocol(
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SyncApiProtocol {

    override fun pushUrl(op: SyncOperation): String = when (op.type) {
        OperationType.Create -> "/api/v1/sync"
        OperationType.Update -> "/api/v1/sync/${op.entityId}"
        OperationType.Delete -> "/api/v1/sync/${op.entityId}"
    }

    override fun pullUrl(cursor: String?): String = buildString {
        append("/api/v1/sync")
        if (cursor != null) append("?cursor=$cursor")
    }

    override suspend fun buildPushRequest(op: SyncOperation): HttpRequestBuilder.() -> Unit = {
        contentType(ContentType.Application.Json)
        if (op.type != OperationType.Delete) {
            val payload = if (op.payload.isNotEmpty()) {
                json.parseToJsonElement(op.payload.decodeToString())
            } else null
            setBody(SyncRequest(entityId = op.entityId, type = op.type.name, payload = payload))
        }
    }

    override suspend fun parsePushResponse(response: HttpResponse): PushResult {
        return try {
            when {
                response.status.isSuccess() -> {
                    val body = response.body<SyncResponse>()
                    PushResult.Success(version = body.version)
                }
                response.status.value == 409 -> {
                    val body = response.body<SyncConflictResponse>()
                    PushResult.Conflict(
                        remoteEntity = SyncEntity(
                            id = body.entity.id,
                            version = body.entity.version,
                            updatedAt = Clock.System.now(),
                            deleted = body.entity.deleted,
                            syncState = SyncState.Synced,
                            payload = json.encodeToString(JsonElement.serializer(), body.entity.payload).encodeToByteArray(),
                        )
                    )
                }
                else -> {
                    PushResult.Error(SyncError.ServerError(response.status.value, response.bodyAsText()))
                }
            }
        } catch (e: Exception) {
            PushResult.Error(SyncError.Unknown(e))
        }
    }

    override suspend fun parsePullResponse(response: HttpResponse): PullResult {
        return try {
            if (response.status.isSuccess()) {
                val body = response.body<SyncPullResponse>()
                PullResult(
                    entities = body.entities.map { it.toSyncEntity() },
                    nextCursor = body.nextCursor,
                )
            } else {
                PullResult(entities = emptyList(), nextCursor = null)
            }
        } catch (e: Exception) {
            PullResult(entities = emptyList(), nextCursor = null)
        }
    }

    private fun RemoteEntity.toSyncEntity() = SyncEntity(
        id = id,
        version = version,
        updatedAt = Clock.System.now(),
        deleted = deleted,
        syncState = SyncState.Synced,
        payload = json.encodeToString(JsonElement.serializer(), payload).encodeToByteArray(),
    )
}

@Serializable
private data class SyncRequest(
    val entityId: String,
    val type: String,
    val payload: JsonElement? = null,
)

@Serializable
private data class SyncResponse(
    val version: Long,
)

@Serializable
private data class SyncConflictResponse(
    val entity: RemoteEntity,
)

@Serializable
private data class SyncPullResponse(
    val entities: List<RemoteEntity>,
    val nextCursor: String? = null,
)

@Serializable
private data class RemoteEntity(
    val id: String,
    val version: Long,
    val deleted: Boolean = false,
    val payload: JsonElement,
)
