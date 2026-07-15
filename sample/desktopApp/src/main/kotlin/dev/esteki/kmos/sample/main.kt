package dev.esteki.kmos.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kmos Todo Sync",
    ) {
        App()
    }
}
