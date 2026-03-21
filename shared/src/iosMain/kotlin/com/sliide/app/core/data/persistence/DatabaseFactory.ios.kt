package com.sliide.app.core.data.persistence

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/sliide.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
    )
}
