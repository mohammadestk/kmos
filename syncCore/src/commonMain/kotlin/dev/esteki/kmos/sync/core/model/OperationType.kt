package dev.esteki.kmos.sync.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class OperationType {
    Create,
    Update,
    Delete
}
