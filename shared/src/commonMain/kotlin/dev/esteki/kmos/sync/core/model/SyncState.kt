package dev.esteki.kmos.sync.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SyncState {
    LocalOnly,
    PendingUpload,
    Synced,
    Conflict,
    Failed
}
