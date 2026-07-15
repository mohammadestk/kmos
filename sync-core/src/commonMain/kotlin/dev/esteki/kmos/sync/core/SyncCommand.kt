package dev.esteki.kmos.sync.core

internal sealed class SyncCommand {
    data object TriggerSync : SyncCommand()
    data object PullAndSync : SyncCommand()
    data object Cancel : SyncCommand()
}
