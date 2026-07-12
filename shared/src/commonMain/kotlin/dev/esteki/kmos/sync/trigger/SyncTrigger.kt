package dev.esteki.kmos.sync.trigger

import kotlin.time.Duration

interface SyncTrigger {
    fun onForeground()
    fun trigger()
    fun startInterval(interval: Duration)
    fun stopInterval()
}
