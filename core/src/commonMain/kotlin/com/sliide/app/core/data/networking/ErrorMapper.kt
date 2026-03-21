package com.sliide.app.core.data.networking

import com.sliide.app.core.common.DomainError
import io.ktor.client.plugins.HttpRequestTimeoutException

fun Throwable.toDomainError(): DomainError = when (this) {
    is HttpRequestTimeoutException -> DomainError.Timeout
    else -> {
        if (isNetworkError()) DomainError.NetworkUnavailable
        else DomainError.Unknown(this)
    }
}

expect fun Throwable.isNetworkError(): Boolean

fun Int.toHttpDomainError(): DomainError = when (this) {
    401 -> DomainError.Unauthorized
    404 -> DomainError.NotFound
    429 -> DomainError.RateLimited
    in 500..599 -> DomainError.ServerError(this, "Server error")
    else -> DomainError.Unknown()
}
