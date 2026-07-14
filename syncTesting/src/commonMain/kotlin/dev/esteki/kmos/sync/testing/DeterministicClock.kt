package dev.esteki.kmos.sync.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeterministicClock(
    initialTime: Long = 0L,
) {
    private val _currentTime = MutableStateFlow(initialTime)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    fun advanceBy(millis: Long) {
        _currentTime.update { it + millis }
    }

    fun setTime(millis: Long) {
        _currentTime.value = millis
    }

    fun now(): Long = _currentTime.value
}
