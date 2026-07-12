package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncOperation

sealed class SyncCommand {
    data class Enqueue(val operation: SyncOperation) : SyncCommand()
    data object TriggerSync : SyncCommand()
    data object Cancel : SyncCommand()
}
