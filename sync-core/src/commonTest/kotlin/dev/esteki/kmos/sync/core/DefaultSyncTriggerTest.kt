package dev.esteki.kmos.sync.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSyncTriggerTest {

    @Test
    fun triggerCallsOnTrigger() = runTest {
        var triggerCount = 0
        val trigger = DefaultSyncTrigger(
            scope = this,
            onTrigger = { triggerCount++ },
        )

        trigger.trigger()
        advanceUntilIdle()

        assertEquals(1, triggerCount)
    }

    @Test
    fun onForegroundCallsTrigger() = runTest {
        var triggerCount = 0
        val trigger = DefaultSyncTrigger(
            scope = this,
            onTrigger = { triggerCount++ },
        )

        trigger.onForeground()
        advanceUntilIdle()

        assertEquals(1, triggerCount)
    }

    @Test
    fun startIntervalTriggersPeriodically() = runTest {
        var triggerCount = 0
        val trigger = DefaultSyncTrigger(
            scope = this,
            onTrigger = { triggerCount++ },
        )

        trigger.startInterval(1.seconds)
        advanceTimeBy(3500)
        trigger.stopInterval()

        assertEquals(3, triggerCount)
    }

    @Test
    fun stopIntervalStopsTriggers() = runTest {
        var triggerCount = 0
        val trigger = DefaultSyncTrigger(
            scope = this,
            onTrigger = { triggerCount++ },
        )

        trigger.startInterval(1.seconds)
        advanceTimeBy(1500)
        trigger.stopInterval()
        advanceTimeBy(2000)

        assertEquals(1, triggerCount)
    }
}
