package com.sliide.app.core.common

sealed interface DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(data))
    is DomainResult.Failure -> this
}

fun <T> Result<T>.toDomainResult(): DomainResult<T> = fold(
    onSuccess = { DomainResult.Success(it) },
    onFailure = { DomainResult.Failure(DomainError.Unknown(it)) },
)
