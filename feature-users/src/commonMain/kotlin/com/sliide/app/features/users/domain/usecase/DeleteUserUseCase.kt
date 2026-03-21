package com.sliide.app.features.users.domain.usecase

import com.sliide.app.core.common.DomainResult
import com.sliide.app.features.users.domain.repository.UserRepository

class DeleteUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: Long): DomainResult<Unit> =
        repository.deleteUser(userId)
}
