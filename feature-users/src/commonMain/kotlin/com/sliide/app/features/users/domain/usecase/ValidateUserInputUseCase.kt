package com.sliide.app.features.users.domain.usecase

import com.sliide.app.core.common.DomainError
import com.sliide.app.core.common.DomainResult
import com.sliide.app.core.common.Violation

class ValidateUserInputUseCase {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    operator fun invoke(name: String, email: String): DomainResult<Unit> {
        val violations = mutableListOf<Violation>()

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            violations.add(Violation("name", "Name is required"))
        } else if (trimmedName.length < 2) {
            violations.add(Violation("name", "Name must be at least 2 characters"))
        }

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            violations.add(Violation("email", "Email is required"))
        } else if (!emailRegex.matches(trimmedEmail)) {
            violations.add(Violation("email", "Invalid email format"))
        }

        return if (violations.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Failure(DomainError.ValidationFailed(violations))
        }
    }
}
