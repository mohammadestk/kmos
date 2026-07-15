package dev.esteki.kmos.sync.storage

import androidx.room3.Room
import dev.esteki.kmos.worker.createSQLiteWasmWorker

actual fun createDatabase(name: String): SyncDatabase {
    return Room.databaseBuilder<SyncDatabase>(name = name)
        .setDriver(createSQLiteWasmWorker())
        .fallbackToDestructiveMigration(dropAllTables = true).build()
}