package com.sliide.app.core.data.networking

actual fun Throwable.isNetworkError(): Boolean {
    val name = this::class.simpleName ?: ""
    return name.contains("DarwinHttpRequestException") ||
        name.contains("ConnectTimeoutException") ||
        name.contains("IOException") ||
        name.contains("ConnectException") ||
        name.contains("SocketException")
}
