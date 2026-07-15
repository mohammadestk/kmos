package dev.esteki.kmos.sync.core

import dev.esteki.kmos.sync.core.model.SyncOperation

internal sealed class SyncCommand {
    data class Enqueue(val operation: SyncOperation) : SyncCommand()
    data object TriggerSync : SyncCommand()
    data object PullAndSync : SyncCommand()
    data object Cancel : SyncCommand()
}
