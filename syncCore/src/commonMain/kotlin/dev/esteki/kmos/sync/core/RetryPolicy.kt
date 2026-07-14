package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncError
import kotlin.time.Duration

interface RetryPolicy {
    fun nextDelay(attempt: Int): Duration
    fun shouldDeadLetter(attempt: Int, lastError: SyncError): Boolean
}
