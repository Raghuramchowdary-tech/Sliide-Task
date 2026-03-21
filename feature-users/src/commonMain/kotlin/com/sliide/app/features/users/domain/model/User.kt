package com.sliide.app.features.users.domain.model

import kotlinx.datetime.Instant

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val gender: Gender,
    val status: UserStatus,
    val createdAt: Instant,
)

enum class Gender { Male, Female }

enum class UserStatus { Active, Inactive }

data class CreateUserRequest(
    val name: String,
    val email: String,
    val gender: Gender,
    val status: UserStatus = UserStatus.Active,
)
