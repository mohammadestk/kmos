package dev.esteki.kmos.sync.core.model

sealed class SyncError {
    data object NetworkTimeout : SyncError()
    data class ServerError(val code: Int, val message: String) : SyncError()
    data object SerializationError : SyncError()
    data object ConflictDetected : SyncError()
    data class Unknown(val cause: Throwable? = null) : SyncError()
}
