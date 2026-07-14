package dev.esteki.kmos

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.esteki.kmos.sync.syncModule
import dev.esteki.kmos.viewmodel.TodoViewModel
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get

fun main() = application {
    startKoin {
        modules(syncModule())
    }

    val viewModel: TodoViewModel = get(TodoViewModel::class.java)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Kmos Todo Sync",
    ) {
        App(viewModel = viewModel)
    }
}
