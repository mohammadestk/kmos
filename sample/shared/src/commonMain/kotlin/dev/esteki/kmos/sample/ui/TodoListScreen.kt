package dev.esteki.kmos.sample.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.esteki.kmos.sample.viewmodel.TodoViewModel

@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.items.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val failedOperations by viewModel.failedOperations.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kmos Todo Sync") },
                actions = {
                    TextButton(onClick = { viewModel.syncNow() }) {
                        Text("Sync")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        },
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (items.isEmpty()) {
                Text(
                    text = "No todos yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items, key = { it.id }) { item ->
                        TodoItemRow(
                            title = item.title,
                            completed = item.completed,
                            onToggle = { viewModel.toggleComplete(item) },
                            onDelete = { viewModel.deleteTodo(item) },
                        )
                    }
                }
            }

            SyncStatusBar(
                progress = syncProgress,
                failedOperations = failedOperations,
                onRetry = { viewModel.retryFailed(it) },
                onDiscard = { viewModel.discardFailed(it) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                viewModel.addTodo(title)
                showAddDialog = false
            },
        )
    }
}
