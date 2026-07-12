package dev.esteki.kmos.sync.network

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncOperation

class KtorTransportAdapter : TransportAdapter {

    override suspend fun push(op: SyncOperation): PushResult {
        // Placeholder implementation - will be implemented with actual Ktor client
        return PushResult.Success(version = 1L)
    }

    override suspend fun pull(cursor: String?): PullResult {
        // Placeholder implementation - will be implemented with actual Ktor client
        return PullResult(entities = emptyList(), nextCursor = null)
    }
}
