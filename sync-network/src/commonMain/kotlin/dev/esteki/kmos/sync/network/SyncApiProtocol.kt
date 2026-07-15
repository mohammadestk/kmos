package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock

@Serializable
internal data class RestObject(
    val id: String,
    val name: String,
    val data: JsonElement? = null,
)

@Serializable
internal data class CreateObjectRequest(
    val name: String,
    val data: JsonElement? = null,
)

@Serializable
internal data class UpdateObjectRequest(
    val name: String,
    val data: JsonElement? = null,
)

@Serializable
internal data class DeleteResponse(
    val message: String,
)

internal fun RestObject.toSyncEntity() = SyncEntity(
    id = id,
    version = 1L,
    updatedAt = Clock.System.now(),
    deleted = false,
    syncState = SyncState.Synced,
    payload = data.toString().encodeToByteArray(),
)
