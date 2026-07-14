package dev.esteki.kmos.sync.core

interface ConflictResolver<T> {
    fun resolve(local: T, remote: T): T
}
