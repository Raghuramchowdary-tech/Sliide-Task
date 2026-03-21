package com.sliide.app.features.users.domain.usecase

import com.sliide.app.core.common.DomainResult
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.repository.UserRepository

class AddUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(request: CreateUserRequest): DomainResult<User> =
        repository.addUser(request)
}
