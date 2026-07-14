package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncOperation

interface TransportAdapter {
    suspend fun push(op: SyncOperation): PushResult
    suspend fun pull(cursor: String?): PullResult
}
