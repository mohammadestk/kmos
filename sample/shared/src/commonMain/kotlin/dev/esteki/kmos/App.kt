package dev.esteki.kmos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.esteki.kmos.sync.syncModule
import dev.esteki.kmos.ui.TodoListScreen
import dev.esteki.kmos.viewmodel.TodoViewModel
import org.koin.core.context.GlobalContext

@Composable
fun App() {
    val viewModel = remember {
        val koin = if (GlobalContext.getOrNull() != null) {
            GlobalContext.get()
        } else {
            GlobalContext.startKoin {
                modules(syncModule())
            }.koin
        }
        koin.get<TodoViewModel>()
    }

    TodoListScreen(viewModel = viewModel)
}
