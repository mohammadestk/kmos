package dev.esteki.kmos

import androidx.compose.runtime.Composable
import dev.esteki.kmos.ui.TodoListScreen
import dev.esteki.kmos.viewmodel.TodoViewModel
import org.koin.compose.koinInject

@Composable
fun App() {
    val viewModel: TodoViewModel = koinInject()
    TodoListScreen(viewModel = viewModel)
}
