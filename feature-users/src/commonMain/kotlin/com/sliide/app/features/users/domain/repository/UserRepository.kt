package com.sliide.app.features.users.domain.repository

import com.sliide.app.core.common.DomainResult
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUsers(): Flow<List<User>>
    suspend fun refreshFromLastPage(): DomainResult<Unit>
    suspend fun addUser(request: CreateUserRequest): DomainResult<User>
    suspend fun deleteUser(userId: Long): DomainResult<Unit>
    suspend fun restoreUser(user: User): DomainResult<Unit>
}
