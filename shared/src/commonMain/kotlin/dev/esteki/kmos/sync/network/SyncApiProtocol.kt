package dev.esteki.kmos.sync.network

import kotlinx.serialization.Serializable

@Serializable
data class PushRequest(
    val operationId: String,
    val entityId: String,
    val type: String,
    val attempt: Int,
    val payload: String,
)

@Serializable
data class PushResponse(
    val success: Boolean,
    val version: Long? = null,
    val error: String? = null,
)

@Serializable
data class PullRequest(
    val cursor: String? = null,
)

@Serializable
data class PullResponse(
    val entities: List<EntityDto>,
    val nextCursor: String? = null,
)

@Serializable
data class EntityDto(
    val id: String,
    val version: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val syncState: String,
    val payload: String,
)
