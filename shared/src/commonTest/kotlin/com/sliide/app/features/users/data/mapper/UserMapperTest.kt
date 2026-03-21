package com.sliide.app.features.users.data.mapper

import com.sliide.app.features.users.data.local.UserEntity
import com.sliide.app.features.users.data.remote.UserDto
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.Gender
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.model.UserStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserMapperTest {

    private val mapper = UserMapper()

    // --- dtoToEntity ---

    @Test
    fun `dtoToEntity parses valid ISO date into epoch millis`() {
        val isoDate = "2024-06-15T10:30:00Z"
        val dto = UserDto(
            id = 1L,
            name = "Alice",
            email = "alice@example.com",
            gender = "female",
            status = "active",
            created_at = isoDate,
        )

        val entity = mapper.dtoToEntity(dto)

        assertEquals(1L, entity.id)
        assertEquals("Alice", entity.name)
        assertEquals("alice@example.com", entity.email)
        assertEquals("female", entity.gender)
        assertEquals("active", entity.status)
        assertEquals(Instant.parse(isoDate).toEpochMilliseconds(), entity.createdAtEpoch)
    }

    @Test
    fun `dtoToEntity with null created_at falls back to Clock System now`() {
        val before = Clock.System.now().toEpochMilliseconds()
        val dto = UserDto(
            id = 2L,
            name = "Bob",
            email = "bob@example.com",
            gender = "male",
            status = "active",
            created_at = null,
        )

        val entity = mapper.dtoToEntity(dto)
        val after = Clock.System.now().toEpochMilliseconds()

        assertTrue(
            entity.createdAtEpoch in before..after,
            "Expected createdAtEpoch (${entity.createdAtEpoch}) to be between $before and $after",
        )
    }

    @Test
    fun `dtoToEntity with malformed date falls back to Clock System now`() {
        val before = Clock.System.now().toEpochMilliseconds()
        val dto = UserDto(
            id = 3L,
            name = "Charlie",
            email = "charlie@example.com",
            gender = "male",
            status = "inactive",
            created_at = "not-a-date",
        )

        val entity = mapper.dtoToEntity(dto)
        val after = Clock.System.now().toEpochMilliseconds()

        assertTrue(
            entity.createdAtEpoch in before..after,
            "Expected createdAtEpoch (${entity.createdAtEpoch}) to be between $before and $after",
        )
    }

    // --- entityToDomain ---

    @Test
    fun `entityToDomain maps female gender correctly`() {
        val entity = UserEntity(
            id = 10L,
            name = "Diana",
            email = "diana@example.com",
            gender = "female",
            status = "active",
            createdAtEpoch = 1_718_451_000_000L,
        )

        val user = mapper.entityToDomain(entity)

        assertEquals(Gender.Female, user.gender)
    }

    @Test
    fun `entityToDomain maps male gender correctly`() {
        val entity = UserEntity(
            id = 11L,
            name = "Edward",
            email = "edward@example.com",
            gender = "male",
            status = "active",
            createdAtEpoch = 1_718_451_000_000L,
        )

        val user = mapper.entityToDomain(entity)

        assertEquals(Gender.Male, user.gender)
    }

    @Test
    fun `entityToDomain maps unknown gender to Male as default`() {
        val entity = UserEntity(
            id = 12L,
            name = "Francis",
            email = "francis@example.com",
            gender = "nonbinary",
            status = "active",
            createdAtEpoch = 1_718_451_000_000L,
        )

        val user = mapper.entityToDomain(entity)

        assertEquals(Gender.Male, user.gender)
    }

    @Test
    fun `entityToDomain maps inactive status correctly`() {
        val entity = UserEntity(
            id = 13L,
            name = "Grace",
            email = "grace@example.com",
            gender = "female",
            status = "inactive",
            createdAtEpoch = 1_718_451_000_000L,
        )

        val user = mapper.entityToDomain(entity)

        assertEquals(UserStatus.Inactive, user.status)
    }

    @Test
    fun `entityToDomain maps active status correctly`() {
        val entity = UserEntity(
            id = 14L,
            name = "Henry",
            email = "henry@example.com",
            gender = "male",
            status = "active",
            createdAtEpoch = 1_718_451_000_000L,
        )

        val user = mapper.entityToDomain(entity)

        assertEquals(UserStatus.Active, user.status)
        assertEquals(14L, user.id)
        assertEquals("Henry", user.name)
        assertEquals("henry@example.com", user.email)
        assertEquals(Instant.fromEpochMilliseconds(1_718_451_000_000L), user.createdAt)
    }

    // --- domainToEntity round-trip ---

    @Test
    fun `domainToEntity and entityToDomain round-trip preserves fidelity`() {
        val originalUser = User(
            id = 20L,
            name = "Irene",
            email = "irene@example.com",
            gender = Gender.Female,
            status = UserStatus.Inactive,
            createdAt = Instant.parse("2024-01-15T08:00:00Z"),
        )

        val entity = mapper.domainToEntity(originalUser)
        val roundTrippedUser = mapper.entityToDomain(entity)

        assertEquals(originalUser.id, roundTrippedUser.id)
        assertEquals(originalUser.name, roundTrippedUser.name)
        assertEquals(originalUser.email, roundTrippedUser.email)
        assertEquals(originalUser.gender, roundTrippedUser.gender)
        assertEquals(originalUser.status, roundTrippedUser.status)
        assertEquals(originalUser.createdAt, roundTrippedUser.createdAt)
    }

    // --- createRequestToBody ---

    @Test
    fun `createRequestToBody maps all fields correctly`() {
        val request = CreateUserRequest(
            name = "Jack",
            email = "jack@example.com",
            gender = Gender.Male,
            status = UserStatus.Active,
        )

        val body = mapper.createRequestToBody(request)

        assertEquals("Jack", body.name)
        assertEquals("jack@example.com", body.email)
        assertEquals("male", body.gender)
        assertEquals("active", body.status)
    }
}
