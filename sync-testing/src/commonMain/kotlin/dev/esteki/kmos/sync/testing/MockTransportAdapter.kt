package dev.esteki.kmos.sync.testing

import dev.esteki.kmos.sync.core.TransportAdapter
import dev.esteki.kmos.sync.core.model.PullResult
import dev.esteki.kmos.sync.core.model.PushResult
import dev.esteki.kmos.sync.core.model.SyncOperation
import kotlin.time.Duration

class MockTransportAdapter : TransportAdapter {
    var pushResult: PushResult = PushResult.Success(1L)
    var pullResult: PullResult = PullResult(emptyList(), null)
    var pushDelay: Duration = Duration.ZERO
    var pullDelay: Duration = Duration.ZERO

    val pushCalls = mutableListOf<SyncOperation>()
    val pullCalls = mutableListOf<String?>()

    override suspend fun push(op: SyncOperation): PushResult {
        pushCalls.add(op)
        if (pushDelay > Duration.ZERO) {
            kotlinx.coroutines.delay(pushDelay)
        }
        return pushResult
    }

    override suspend fun pull(cursor: String?): PullResult {
        pullCalls.add(cursor)
        if (pullDelay > Duration.ZERO) {
            kotlinx.coroutines.delay(pullDelay)
        }
        return pullResult
    }

    fun reset() {
        pushCalls.clear()
        pullCalls.clear()
        pushResult = PushResult.Success(1L)
        pullResult = PullResult(emptyList(), null)
        pushDelay = Duration.ZERO
        pullDelay = Duration.ZERO
    }
}
