package dev.esteki.kmos

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform