package dev.esteki.kmos.sync.storage

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [SyncEntityTable::class], version = 1)
abstract class SyncDatabase : RoomDatabase() {
    abstract fun syncEntityDao(): SyncEntityDao
}
