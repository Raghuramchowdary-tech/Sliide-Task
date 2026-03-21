package com.sliide.app.core.data.networking

import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException

actual fun Throwable.isNetworkError(): Boolean =
    this is IOException ||
        this is ConnectException ||
        this is UnknownHostException ||
        this is SocketException
