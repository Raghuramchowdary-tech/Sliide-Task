package com.sliide.app.features.users.data.remote

import com.sliide.app.core.common.DomainError
import com.sliide.app.core.common.DomainResult
import com.sliide.app.core.common.Violation
import com.sliide.app.core.data.networking.toDomainError
import com.sliide.app.core.data.networking.toHttpDomainError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

internal class UserRemoteDataSource(private val client: HttpClient) {

    suspend fun fetchUsers(page: Int, perPage: Int = 20): DomainResult<Pair<List<UserDto>, Int>> {
        return try {
            val response: HttpResponse = client.get("users") {
                parameter("page", page)
                parameter("per_page", perPage)
            }
            if (!response.status.isSuccess()) {
                return DomainResult.Failure(response.status.value.toHttpDomainError())
            }
            val users: List<UserDto> = response.body()
            val totalPages = response.headers["x-pagination-pages"]?.toIntOrNull() ?: 1
            DomainResult.Success(users to totalPages)
        } catch (e: Exception) {
            DomainResult.Failure(e.toDomainError())
        }
    }

    suspend fun fetchLastPageUsers(perPage: Int = 20): DomainResult<List<UserDto>> {
        return when (val firstResult = fetchUsers(page = 1, perPage = perPage)) {
            is DomainResult.Failure -> firstResult
            is DomainResult.Success -> {
                val totalPages = firstResult.data.second
                if (totalPages <= 1) {
                    DomainResult.Success(firstResult.data.first)
                } else {
                    when (val lastResult = fetchUsers(page = totalPages, perPage = perPage)) {
                        is DomainResult.Failure -> lastResult
                        is DomainResult.Success -> DomainResult.Success(lastResult.data.first)
                    }
                }
            }
        }
    }

    suspend fun createUser(body: CreateUserBody): DomainResult<UserDto> {
        return try {
            val response: HttpResponse = client.post("users") {
                setBody(body)
            }
            when (response.status) {
                HttpStatusCode.Created -> DomainResult.Success(response.body<UserDto>())
                HttpStatusCode.UnprocessableEntity -> {
                    val errors: List<ApiValidationError> = response.body()
                    DomainResult.Failure(
                        DomainError.ValidationFailed(
                            errors.map { Violation(it.field, it.message) }
                        )
                    )
                }
                else -> DomainResult.Failure(DomainError.Unknown())
            }
        } catch (e: Exception) {
            DomainResult.Failure(e.toDomainError())
        }
    }

    suspend fun deleteUser(userId: Long): DomainResult<Unit> {
        return try {
            val response: HttpResponse = client.delete("users/$userId")
            when (response.status) {
                HttpStatusCode.NoContent -> DomainResult.Success(Unit)
                HttpStatusCode.NotFound -> DomainResult.Failure(DomainError.NotFound)
                else -> DomainResult.Failure(DomainError.Unknown())
            }
        } catch (e: Exception) {
            DomainResult.Failure(e.toDomainError())
        }
    }
}
