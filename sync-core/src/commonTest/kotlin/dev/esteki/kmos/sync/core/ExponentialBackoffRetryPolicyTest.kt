package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ExponentialBackoffRetryPolicyTest {

    private val policy = ExponentialBackoffRetryPolicy(
        baseDelay = 1000.milliseconds,
        maxDelay = 30_000.milliseconds,
        maxAttempts = 3,
    )

    @Test
    fun delaysGrowExponentially() {
        val delay0 = policy.nextDelay(0)
        val delay1 = policy.nextDelay(1)
        val delay2 = policy.nextDelay(2)

        // Base delay is 1000ms, so attempt 0 should be around 1000ms (+ jitter)
        assertTrue(delay0.inWholeMilliseconds >= 1000)
        assertTrue(delay0.inWholeMilliseconds < 1500)

        // Attempt 1 should be around 2000ms (+ jitter)
        assertTrue(delay1.inWholeMilliseconds >= 2000)
        assertTrue(delay1.inWholeMilliseconds < 3000)

        // Attempt 2 should be around 4000ms (+ jitter)
        assertTrue(delay2.inWholeMilliseconds >= 4000)
        assertTrue(delay2.inWholeMilliseconds < 6000)
    }

    @Test
    fun delaysAreCappedAtMaxDelay() {
        // With baseDelay=1000ms and maxDelay=30000ms, attempt 5 would be 32000ms without cap
        val delay = policy.nextDelay(5)
        assertTrue(delay.inWholeMilliseconds <= 30_000)
    }

    @Test
    fun deadLetterTriggersAtMaxAttempts() {
        assertFalse(policy.shouldDeadLetter(0, SyncError.NetworkTimeout))
        assertFalse(policy.shouldDeadLetter(1, SyncError.NetworkTimeout))
        assertFalse(policy.shouldDeadLetter(2, SyncError.NetworkTimeout))
        assertTrue(policy.shouldDeadLetter(3, SyncError.NetworkTimeout))
    }

    @Test
    fun conflictDetectedAlwaysDeadLetters() {
        assertTrue(policy.shouldDeadLetter(0, SyncError.ConflictDetected))
        assertTrue(policy.shouldDeadLetter(1, SyncError.ConflictDetected))
    }

    @Test
    fun serverErrorDoesNotDeadLetterBeforeMaxAttempts() {
        assertFalse(policy.shouldDeadLetter(0, SyncError.ServerError(500, "Internal Server Error")))
        assertFalse(policy.shouldDeadLetter(2, SyncError.ServerError(500, "Internal Server Error")))
        assertTrue(policy.shouldDeadLetter(3, SyncError.ServerError(500, "Internal Server Error")))
    }
}
