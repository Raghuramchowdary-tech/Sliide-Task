package com.sliide.app.core.data.persistence

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.sliide.app.features.users.data.local.PendingDeleteDao
import com.sliide.app.features.users.data.local.PendingDeleteEntity
import com.sliide.app.features.users.data.local.UserDao
import com.sliide.app.features.users.data.local.UserEntity

@Database(entities = [UserEntity::class, PendingDeleteEntity::class], version = 3)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun pendingDeleteDao(): PendingDeleteDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
