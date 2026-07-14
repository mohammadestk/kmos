package dev.esteki.kmos.sync.core.model

data class PullResult(
    val entities: List<SyncEntity>,
    val nextCursor: String?,
)
