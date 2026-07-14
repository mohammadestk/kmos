package dev.esteki.kmos.sync.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual fun createDatabase(name: String): SyncDatabase {
    return Room.databaseBuilder<SyncDatabase>(
        name = name,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
        .build()
}

actual fun createInMemoryDatabase(): SyncDatabase {
    return Room.inMemoryDatabaseBuilder<SyncDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
        .build()
}
