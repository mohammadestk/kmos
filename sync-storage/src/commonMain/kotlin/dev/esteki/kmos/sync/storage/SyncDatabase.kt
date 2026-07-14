package dev.esteki.kmos.sync.storage

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [SyncEntityTable::class, SyncOperationTable::class], version = 2)
abstract class SyncDatabase : RoomDatabase() {
    abstract fun syncEntityDao(): SyncEntityDao
    abstract fun syncOperationDao(): SyncOperationDao
}
