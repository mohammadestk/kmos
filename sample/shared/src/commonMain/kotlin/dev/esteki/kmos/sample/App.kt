package dev.esteki.kmos.sample

import androidx.compose.runtime.Composable
import dev.esteki.kmos.sample.sync.appModule
import dev.esteki.kmos.sample.ui.TodoListScreen
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                modules(appModule)
            },
        ),
        content = {
            TodoListScreen(viewModel = koinViewModel())
        }
    )
}
