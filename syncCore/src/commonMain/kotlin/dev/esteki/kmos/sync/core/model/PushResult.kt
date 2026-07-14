package dev.esteki.kmos.sync.core.model

sealed class PushResult {
    data class Success(val version: Long) : PushResult()
    data class Conflict(val remoteEntity: SyncEntity) : PushResult()
    data class Error(val error: SyncError) : PushResult()
}
