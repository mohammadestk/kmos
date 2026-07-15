package dev.esteki.kmos.sync.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

class DefaultSyncTrigger(
    private val scope: CoroutineScope,
    private val onTrigger: suspend () -> Unit,
) : SyncTrigger {

    private var intervalJob: Job? = null

    override fun onForeground() {
        trigger()
    }

    override fun trigger() {
        scope.launch {
            onTrigger()
        }
    }

    override fun startInterval(interval: Duration) {
        stopInterval()
        intervalJob = scope.launch {
            while (true) {
                delay(interval)
                onTrigger()
            }
        }
    }

    override fun stopInterval() {
        intervalJob?.cancel()
        intervalJob = null
    }
}
