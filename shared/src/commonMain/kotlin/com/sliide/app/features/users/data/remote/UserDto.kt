package com.sliide.app.features.users.data.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val gender: String,
    val status: String,
    val created_at: String? = null,
)

@Serializable
internal data class CreateUserBody(
    val name: String,
    val email: String,
    val gender: String,
    val status: String,
)

@Serializable
internal data class ApiValidationError(
    val field: String,
    val message: String,
)
