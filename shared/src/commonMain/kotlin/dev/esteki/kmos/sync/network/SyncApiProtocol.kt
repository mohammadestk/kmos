package dev.esteki.kmos.sync.network

import kotlinx.serialization.Serializable

@Serializable
data class ServerObject(
    val id: String,
    val name: String,
    val data: String,
)
