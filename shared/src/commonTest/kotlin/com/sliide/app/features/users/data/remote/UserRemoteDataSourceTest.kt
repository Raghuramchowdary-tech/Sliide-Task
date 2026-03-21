package com.sliide.app.features.users.data.remote

import com.sliide.app.core.common.DomainError
import com.sliide.app.core.common.DomainResult
import com.sliide.app.core.common.Violation
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.contentType
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserRemoteDataSourceTest {

    private val testUsers = listOf(
        UserDto(
            id = 1L,
            name = "Alice Smith",
            email = "alice@example.com",
            gender = "female",
            status = "active",
        ),
        UserDto(
            id = 2L,
            name = "Bob Jones",
            email = "bob@example.com",
            gender = "male",
            status = "active",
        ),
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun createClient(handler: MockRequestHandler): HttpClient {
        return HttpClient(MockEngine(handler)) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
    }

    // -- fetchUsers --

    @Test
    fun `fetchUsers returns users and totalPages from header`() = runTest {
        val client = createClient { request ->
            assertEquals("users", request.url.encodedPath.trimStart('/'))
            assertEquals("1", request.url.parameters["page"])
            assertEquals("20", request.url.parameters["per_page"])
            respond(
                content = json.encodeToString(testUsers),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    "x-pagination-pages" to listOf("5"),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val result = api.fetchUsers(page = 1, perPage = 20)

        assertIs<DomainResult.Success<Pair<List<UserDto>, Int>>>(result)
        assertEquals(testUsers, result.data.first)
        assertEquals(5, result.data.second)
    }

    @Test
    fun `fetchUsers defaults totalPages to 1 when header missing`() = runTest {
        val client = createClient {
            respond(
                content = json.encodeToString(testUsers),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val result = api.fetchUsers(page = 1)

        assertIs<DomainResult.Success<Pair<List<UserDto>, Int>>>(result)
        assertEquals(testUsers, result.data.first)
        assertEquals(1, result.data.second)
    }

    @Test
    fun `fetchUsers returns Unauthorized on 401`() = runTest {
        val client = createClient {
            respond(
                content = "",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val result = api.fetchUsers(page = 1)

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.Unauthorized, result.error)
    }

    @Test
    fun `fetchUsers returns ServerError on 500`() = runTest {
        val client = createClient {
            respond(
                content = "",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val result = api.fetchUsers(page = 1)

        assertIs<DomainResult.Failure>(result)
        val error = result.error
        assertIs<DomainError.ServerError>(error)
        assertEquals(500, error.code)
    }

    // -- fetchLastPageUsers --

    @Test
    fun `fetchLastPageUsers returns first page when totalPages is 1`() = runTest {
        val client = createClient {
            respond(
                content = json.encodeToString(testUsers),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    "x-pagination-pages" to listOf("1"),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val result = api.fetchLastPageUsers()

        assertIs<DomainResult.Success<List<UserDto>>>(result)
        assertEquals(testUsers, result.data)
    }

    @Test
    fun `fetchLastPageUsers fetches last page when totalPages greater than 1`() = runTest {
        val lastPageUsers = listOf(
            UserDto(
                id = 99L,
                name = "Zara Last",
                email = "zara@example.com",
                gender = "female",
                status = "active",
            ),
        )
        var requestCount = 0
        val client = createClient { request ->
            requestCount++
            val page = request.url.parameters["page"]
            when (page) {
                "1" -> respond(
                    content = json.encodeToString(testUsers),
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                        "x-pagination-pages" to listOf("3"),
                    ),
                )
                "3" -> respond(
                    content = json.encodeToString(lastPageUsers),
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                        "x-pagination-pages" to listOf("3"),
                    ),
                )
                else -> error("Unexpected page: $page")
            }
        }
        val api = UserRemoteDataSource(client)
        val result = api.fetchLastPageUsers()

        assertIs<DomainResult.Success<List<UserDto>>>(result)
        assertEquals(lastPageUsers, result.data)
        assertEquals(2, requestCount)
    }

    // -- createUser --

    @Test
    fun `createUser returns user on 201 Created`() = runTest {
        val createdUser = UserDto(
            id = 10L,
            name = "New User",
            email = "new@example.com",
            gender = "male",
            status = "active",
        )
        val client = createClient { request ->
            assertEquals(HttpMethod.Post, request.method)
            respond(
                content = json.encodeToString(createdUser),
                status = HttpStatusCode.Created,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val body = CreateUserBody(
            name = "New User",
            email = "new@example.com",
            gender = "male",
            status = "active",
        )
        val result = api.createUser(body)

        assertIs<DomainResult.Success<UserDto>>(result)
        assertEquals(createdUser, result.data)
    }

    @Test
    fun `createUser returns ValidationFailed on 422`() = runTest {
        val validationErrors = listOf(
            ApiValidationError(field = "email", message = "has already been taken"),
            ApiValidationError(field = "name", message = "can't be blank"),
        )
        val client = createClient {
            respond(
                content = json.encodeToString(validationErrors),
                status = HttpStatusCode.UnprocessableEntity,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val body = CreateUserBody(
            name = "",
            email = "duplicate@example.com",
            gender = "male",
            status = "active",
        )
        val result = api.createUser(body)

        assertIs<DomainResult.Failure>(result)
        val error = result.error
        assertIs<DomainError.ValidationFailed>(error)
        assertEquals(
            listOf(
                Violation("email", "has already been taken"),
                Violation("name", "can't be blank"),
            ),
            error.violations,
        )
    }

    // -- deleteUser --

    @Test
    fun `deleteUser returns success on 204 NoContent`() = runTest {
        val client = createClient { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("users/42", request.url.encodedPath.trimStart('/'))
            respond(
                content = "",
                status = HttpStatusCode.NoContent,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val result = api.deleteUser(userId = 42L)

        assertIs<DomainResult.Success<Unit>>(result)
    }

    @Test
    fun `deleteUser returns NotFound on 404`() = runTest {
        val client = createClient {
            respond(
                content = "",
                status = HttpStatusCode.NotFound,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                ),
            )
        }
        val api = UserRemoteDataSource(client)
        val result = api.deleteUser(userId = 999L)

        assertIs<DomainResult.Failure>(result)
        assertEquals(DomainError.NotFound, result.error)
    }
}
