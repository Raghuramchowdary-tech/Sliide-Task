package com.sliide.app.features.users.data.mapper

import com.sliide.app.features.users.data.local.UserEntity
import com.sliide.app.features.users.data.remote.CreateUserBody
import com.sliide.app.features.users.data.remote.UserDto
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.Gender
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.model.UserStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal class UserMapper {

    fun dtoToEntity(dto: UserDto): UserEntity = UserEntity(
        id = dto.id,
        name = dto.name,
        email = dto.email,
        gender = dto.gender,
        status = dto.status,
        createdAtEpoch = dto.created_at?.let {
            try { Instant.parse(it).toEpochMilliseconds() }
            catch (_: Exception) { null }
        } ?: Clock.System.now().toEpochMilliseconds(),
    )

    fun entityToDomain(entity: UserEntity): User = User(
        id = entity.id,
        name = entity.name,
        email = entity.email,
        gender = when (entity.gender.lowercase()) {
            "female" -> Gender.Female
            else -> Gender.Male
        },
        status = when (entity.status.lowercase()) {
            "inactive" -> UserStatus.Inactive
            else -> UserStatus.Active
        },
        createdAt = Instant.fromEpochMilliseconds(entity.createdAtEpoch),
    )

    fun domainToEntity(user: User): UserEntity = UserEntity(
        id = user.id,
        name = user.name,
        email = user.email,
        gender = user.gender.name.lowercase(),
        status = user.status.name.lowercase(),
        createdAtEpoch = user.createdAt.toEpochMilliseconds(),
    )

    fun createRequestToBody(request: CreateUserRequest): CreateUserBody = CreateUserBody(
        name = request.name,
        email = request.email,
        gender = request.gender.name.lowercase(),
        status = request.status.name.lowercase(),
    )
}
