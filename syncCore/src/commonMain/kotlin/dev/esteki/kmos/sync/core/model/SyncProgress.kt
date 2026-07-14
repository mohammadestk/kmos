package dev.esteki.kmos.sync.core.model

sealed class SyncProgress {
    data object Idle : SyncProgress()
    data class Pushing(
        val operationId: String,
        val completed: Int,
        val total: Int,
    ) : SyncProgress()

    data class Pulling(val cursor: String?) : SyncProgress()
    data class Completed(
        val pushed: Int,
        val pulled: Int,
        val conflicts: Int,
    ) : SyncProgress()

    data class Error(
        val operationId: String,
        val error: SyncError,
    ) : SyncProgress()
}
