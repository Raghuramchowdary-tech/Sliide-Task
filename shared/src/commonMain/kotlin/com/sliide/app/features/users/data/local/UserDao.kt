package com.sliide.app.features.users.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Upsert
    suspend fun upsert(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Query("DELETE FROM users WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)

    @Transaction
    suspend fun replaceAll(users: List<UserEntity>) {
        deleteAll()
        upsert(users)
    }
}
