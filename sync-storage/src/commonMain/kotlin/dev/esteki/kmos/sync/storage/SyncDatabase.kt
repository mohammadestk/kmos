package dev.esteki.kmos.sync.storage

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [SyncEntityTable::class], version = 3)
@ConstructedBy(SyncDatabaseConstructor::class)
abstract class SyncDatabase : RoomDatabase() {
    internal abstract fun syncEntityDao(): SyncEntityDao
}

expect object SyncDatabaseConstructor : RoomDatabaseConstructor<SyncDatabase> {
    override fun initialize(): SyncDatabase
}
