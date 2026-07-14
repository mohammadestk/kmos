package dev.esteki.kmos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.esteki.kmos.sync.core.model.SyncEntity
import dev.esteki.kmos.sync.core.model.SyncProgress

@Composable
fun SyncStatusBar(
    progress: SyncProgress,
    failedOperations: List<SyncEntity>,
    onRetry: (SyncEntity) -> Unit,
    onDiscard: (SyncEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = when (progress) {
                is SyncProgress.Idle -> "Sync: Idle"
                is SyncProgress.Pushing -> "Syncing: Pushing ${progress.completed}/${progress.total}"
                is SyncProgress.Pulling -> "Syncing: Pulling..."
                is SyncProgress.Completed -> "Sync complete: ${progress.pushed} pushed, ${progress.pulled} pulled, ${progress.conflicts} conflicts"
                is SyncProgress.Error -> "Sync error: ${progress.error}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )

        if (failedOperations.isNotEmpty()) {
            Text(
                text = "Failed operations: ${failedOperations.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            failedOperations.forEach { entity ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Failed: ${entity.id.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onRetry(entity) }) {
                        Text("Retry")
                    }
                    TextButton(onClick = { onDiscard(entity) }) {
                        Text("Discard")
                    }
                }
            }
        }
    }
}
