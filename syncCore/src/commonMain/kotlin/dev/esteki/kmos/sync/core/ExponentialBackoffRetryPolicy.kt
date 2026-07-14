package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncError
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ExponentialBackoffRetryPolicy(
    private val baseDelay: Duration = 1000.milliseconds,
    private val maxDelay: Duration = 60_000.milliseconds,
    private val maxAttempts: Int = 5,
    private val jitterFactor: Double = 0.3,
) : RetryPolicy {

    override fun nextDelay(attempt: Int): Duration {
        val exponentialDelay = baseDelay.inWholeMilliseconds * (1L shl attempt)
        val cappedDelay = min(exponentialDelay, maxDelay.inWholeMilliseconds)
        val jitter = cappedDelay * jitterFactor * Random.nextDouble()
        val finalDelay = min((cappedDelay + jitter).toLong(), maxDelay.inWholeMilliseconds)
        return finalDelay.milliseconds
    }

    override fun shouldDeadLetter(attempt: Int, lastError: SyncError): Boolean {
        if (lastError is SyncError.ConflictDetected) return true
        return attempt >= maxAttempts
    }
}
