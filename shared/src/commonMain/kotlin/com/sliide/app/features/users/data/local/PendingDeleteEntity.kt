package com.sliide.app.features.users.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_deletes")
data class PendingDeleteEntity(
    @PrimaryKey val userId: Long,
)
