package dev.esteki.kmos.sync.storage

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
internal interface SyncOperationDao {
    @Query("SELECT * FROM sync_operations")
    suspend fun getAll(): List<SyncOperationTable>

    @Query("SELECT * FROM sync_operations WHERE operationId = :operationId")
    suspend fun getByOperationId(operationId: String): SyncOperationTable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(operation: SyncOperationTable)

    @Query("DELETE FROM sync_operations WHERE operationId = :operationId")
    suspend fun deleteByOperationId(operationId: String)

    @Query("DELETE FROM sync_operations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sync_operations")
    suspend fun count(): Int
}
