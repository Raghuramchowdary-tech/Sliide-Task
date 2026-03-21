package com.sliide.app.features.users.data.repository

import com.sliide.app.core.common.DomainError
import com.sliide.app.core.common.DomainResult
import com.sliide.app.features.users.data.local.PendingDeleteDao
import com.sliide.app.features.users.data.local.PendingDeleteEntity
import com.sliide.app.features.users.data.local.UserDao
import com.sliide.app.features.users.data.local.UserEntity
import com.sliide.app.features.users.data.mapper.UserMapper
import com.sliide.app.features.users.data.remote.UserRemoteDataSource
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.Gender
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.model.UserStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.contentType
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UserRepositoryImplTest {

    private val mapper = UserMapper()
    private val jsonCodec = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun testUser(id: Long = 1L) = User(
        id = id,
        name = "John",
        email = "john@test.com",
        gender = Gender.Male,
        status = UserStatus.Active,
        createdAt = Instant.parse("2026-03-20T12:00:00Z"),
    )

    private val usersJson = """[{"id":1,"name":"John","email":"john@test.com","gender":"male","status":"active","created_at":"2026-03-20T12:00:00Z"},{"id":2,"name":"Jane","email":"jane@test.com","gender":"female","status":"active","created_at":"2026-03-20T12:00:00Z"}]"""
    private val createdUserJson = """{"id":5,"name":"John","email":"john@test.com","gender":"male","status":"active","created_at":"2026-03-20T12:00:00Z"}"""

    private fun jsonHeaders() = headersOf(
        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
        "x-pagination-pages" to listOf("1"),
    )

    private fun createRemoteDataSource(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): UserRemoteDataSource {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = false
            install(ContentNegotiation) { json(jsonCodec) }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
        return UserRemoteDataSource(client)
    }

    // --- Fake DAOs ---

    private class FakeUserDao : UserDao {
        val entities = mutableListOf<UserEntity>()
        private val flow = MutableStateFlow<List<UserEntity>>(emptyList())

        override fun observeAll(): Flow<List<UserEntity>> = flow
        override suspend fun upsert(users: List<UserEntity>) {
            for (user in users) { entities.removeAll { it.id == user.id }; entities.add(user) }
            flow.value = entities.sortedByDescending { it.id }
        }
        override suspend fun insert(user: UserEntity) {
            entities.removeAll { it.id == user.id }; entities.add(user)
            flow.value = entities.sortedByDescending { it.id }
        }
        override suspend fun deleteById(id: Long) {
            entities.removeAll { it.id == id }
            flow.value = entities.sortedByDescending { it.id }
        }
        override suspend fun deleteNotIn(ids: List<Long>) {
            entities.removeAll { it.id !in ids }
            flow.value = entities.sortedByDescending { it.id }
        }
        override suspend fun replaceAll(users: List<UserEntity>) {
            upsert(users); deleteNotIn(users.map { it.id })
        }
    }

    private class FakePendingDeleteDao : PendingDeleteDao {
        val entries = mutableListOf<PendingDeleteEntity>()
        override suspend fun getAll(): List<PendingDeleteEntity> = entries.toList()
        override suspend fun insert(entity: PendingDeleteEntity) {
            if (entries.none { it.userId == entity.userId }) entries.add(entity)
        }
        override suspend fun deleteByUserId(userId: Long) {
            entries.removeAll { it.userId == userId }
        }
    }

    // --- Tests ---

    @Test
    fun `refreshFromLastPage replaces local data with API data`() = runTest {
        val remote = createRemoteDataSource { respond(usersJson, HttpStatusCode.OK, jsonHeaders()) }
        val dao = FakeUserDao()
        val pendingDao = FakePendingDeleteDao()
        val repo = UserRepositoryImpl(remote, dao, pendingDao, mapper)

        val result = repo.refreshFromLastPage()

        assertIs<DomainResult.Success<Unit>>(result)
        assertEquals(2, dao.entities.size)
    }

    @Test
    fun `refreshFromLastPage filters out pending deletes`() = runTest {
        val remote = createRemoteDataSource { respond(usersJson, HttpStatusCode.OK, jsonHeaders()) }
        val dao = FakeUserDao()
        val pendingDao = FakePendingDeleteDao()
        pendingDao.entries.add(PendingDeleteEntity(2))
        val repo = UserRepositoryImpl(remote, dao, pendingDao, mapper)

        repo.refreshFromLastPage()

        assertTrue(dao.entities.none { it.id == 2L })
        assertEquals(1, dao.entities.size)
    }

    @Test
    fun `refreshFromLastPage propagates API failure`() = runTest {
        val remote = createRemoteDataSource { respond("", HttpStatusCode.InternalServerError, jsonHeaders()) }
        val dao = FakeUserDao()
        val pendingDao = FakePendingDeleteDao()
        val repo = UserRepositoryImpl(remote, dao, pendingDao, mapper)

        val result = repo.refreshFromLastPage()

        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `addUser inserts to local DB on success`() = runTest {
        val remote = createRemoteDataSource { respond(createdUserJson, HttpStatusCode.Created, jsonHeaders()) }
        val dao = FakeUserDao()
        val pendingDao = FakePendingDeleteDao()
        val repo = UserRepositoryImpl(remote, dao, pendingDao, mapper)

        val result = repo.addUser(CreateUserRequest("John", "john@test.com", Gender.Male))

        assertIs<DomainResult.Success<User>>(result)
        assertEquals(5L, result.data.id)
        assertEquals(1, dao.entities.size)
    }

    @Test
    fun `addUser propagates API failure without DB changes`() = runTest {
        val remote = createRemoteDataSource { respond("", HttpStatusCode.Unauthorized, jsonHeaders()) }
        val dao = FakeUserDao()
        val pendingDao = FakePendingDeleteDao()
        val repo = UserRepositoryImpl(remote, dao, pendingDao, mapper)

        val result = repo.addUser(CreateUserRequest("John", "john@test.com", Gender.Male))

        assertIs<DomainResult.Failure>(result)
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun `deleteUser removes from DB and adds to pending`() = runTest {
        val remote = createRemoteDataSource { respond("", HttpStatusCode.OK, jsonHeaders()) }
        val dao = FakeUserDao()
        val pendingDao = FakePendingDeleteDao()
        dao.entities.add(mapper.domainToEntity(testUser(1)))
        val repo = UserRepositoryImpl(remote, dao, pendingDao, mapper)

        val result = repo.deleteUser(1L)

        assertIs<DomainResult.Success<Unit>>(result)
        assertTrue(dao.entities.isEmpty())
        assertEquals(1, pendingDao.entries.size)
    }

    @Test
    fun `restoreUser removes from pending and re-inserts`() = runTest {
        val remote = createRemoteDataSource { respond("", HttpStatusCode.OK, jsonHeaders()) }
        val dao = FakeUserDao()
        val pendingDao = FakePendingDeleteDao()
        pendingDao.entries.add(PendingDeleteEntity(1))
        val repo = UserRepositoryImpl(remote, dao, pendingDao, mapper)

        val result = repo.restoreUser(testUser(1))

        assertIs<DomainResult.Success<Unit>>(result)
        assertTrue(pendingDao.entries.isEmpty())
        assertEquals(1, dao.entities.size)
    }
}
