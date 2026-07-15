package dev.esteki.kmos.sync.storage

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [SyncEntityTable::class, SyncOperationTable::class], version = 2)
@ConstructedBy(SyncDatabaseConstructor::class)
abstract class SyncDatabase : RoomDatabase() {
    abstract fun syncEntityDao(): SyncEntityDao
    abstract fun syncOperationDao(): SyncOperationDao
}

expect object SyncDatabaseConstructor : RoomDatabaseConstructor<SyncDatabase> {
    override fun initialize(): SyncDatabase
}