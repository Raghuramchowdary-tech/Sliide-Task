package com.sliide.app.core.common

sealed interface DomainError {
    data object NetworkUnavailable : DomainError
    data object Timeout : DomainError
    data class ServerError(val code: Int, val message: String?) : DomainError
    data object Unauthorized : DomainError
    data object NotFound : DomainError
    data class ValidationFailed(val violations: List<Violation>) : DomainError
    data object RateLimited : DomainError
    data class Unknown(val cause: Throwable? = null) : DomainError
}

data class Violation(val field: String, val message: String)
