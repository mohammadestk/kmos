package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncOperation

class FakeTransportAdapter : TransportAdapter {
    var pushResult: PushResult = PushResult.Success(1L)
    var pullResult: PullResult = PullResult(emptyList(), null)

    val pushCalls = mutableListOf<SyncOperation>()
    val pullCalls = mutableListOf<String?>()

    override suspend fun push(op: SyncOperation): PushResult {
        pushCalls.add(op)
        return pushResult
    }

    override suspend fun pull(cursor: String?): PullResult {
        pullCalls.add(cursor)
        return pullResult
    }

    fun clear() {
        pushCalls.clear()
        pullCalls.clear()
    }
}
