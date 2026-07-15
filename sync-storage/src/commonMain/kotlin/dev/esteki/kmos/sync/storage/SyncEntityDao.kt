package dev.esteki.kmos.sync.storage

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SyncEntityDao {
    @Query("SELECT * FROM sync_entities WHERE id = :id")
    suspend fun getById(id: String): SyncEntityTable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncEntityTable)

    @Query("SELECT * FROM sync_entities")
    suspend fun queryAll(): List<SyncEntityTable>

    @Query("SELECT * FROM sync_entities WHERE syncState = 'PendingUpload'")
    suspend fun queryPending(): List<SyncEntityTable>

    @Query("SELECT * FROM sync_entities WHERE syncState = 'Failed'")
    suspend fun queryFailed(): List<SyncEntityTable>

    @Query("DELETE FROM sync_entities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM sync_entities")
    fun observeChangeCount(): Flow<Int>
}
