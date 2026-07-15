package dev.esteki.kmos.sync.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun createDatabase(name: String): SyncDatabase {
    val dbFile = File(System.getProperty("user.dir"), name)

    return Room.databaseBuilder<SyncDatabase>(
        name = dbFile.absolutePath,
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
        .build()
}

fun createInMemoryDatabase(): SyncDatabase {
    return Room.inMemoryDatabaseBuilder<SyncDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
        .build()
}
