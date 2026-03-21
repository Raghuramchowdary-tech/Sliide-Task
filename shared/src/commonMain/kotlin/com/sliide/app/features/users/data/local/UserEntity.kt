package com.sliide.app.features.users.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val email: String,
    val gender: String,
    val status: String,
    val createdAtEpoch: Long,
)
