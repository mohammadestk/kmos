package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock

@Serializable
data class SyncPushRequest(
    val entityId: String,
    val operationType: String,
    val operationId: String,
    val payload: String,
)

@Serializable
data class SyncPushResponse(
    val version: Long,
    val entityId: String,
)

@Serializable
data class SyncPullResponse(
    val entities: List<SyncEntityResponse>,
    val nextCursor: String? = null,
)

@Serializable
data class SyncEntityResponse(
    val id: String,
    val version: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val payload: String,
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun toSyncEntity() = SyncEntity(
        id = id,
        version = version,
        updatedAt = kotlin.time.Instant.fromEpochMilliseconds(updatedAt),
        deleted = deleted,
        syncState = SyncState.Synced,
        payload = Base64.decode(payload),
    )
}


