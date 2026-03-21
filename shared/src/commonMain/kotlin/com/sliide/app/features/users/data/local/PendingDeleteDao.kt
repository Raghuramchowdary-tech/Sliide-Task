package com.sliide.app.features.users.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingDeleteDao {
    @Query("SELECT * FROM pending_deletes")
    suspend fun getAll(): List<PendingDeleteEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingDeleteEntity)

    @Query("DELETE FROM pending_deletes WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Long)
}
