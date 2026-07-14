package dev.esteki.kmos

import androidx.compose.runtime.Composable
import dev.esteki.kmos.ui.TodoListScreen
import dev.esteki.kmos.viewmodel.TodoViewModel

@Composable
fun App(viewModel: TodoViewModel) {
    TodoListScreen(viewModel = viewModel)
}
