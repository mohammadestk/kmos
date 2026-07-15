package dev.esteki.kmos.sync.storage

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

object AndroidContextHolder {
    lateinit var appContext: Context
}

actual fun createDatabase(name: String): SyncDatabase {
    val ctx = AndroidContextHolder.appContext

    return Room.databaseBuilder<SyncDatabase>(
        name = ctx.getDatabasePath(name).absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
        .build()
}
