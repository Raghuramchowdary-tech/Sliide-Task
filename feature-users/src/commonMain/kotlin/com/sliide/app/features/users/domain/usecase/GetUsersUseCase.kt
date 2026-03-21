package com.sliide.app.features.users.domain.usecase

import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetUsersUseCase(private val repository: UserRepository) {
    operator fun invoke(): Flow<List<User>> = repository.observeUsers()
}
