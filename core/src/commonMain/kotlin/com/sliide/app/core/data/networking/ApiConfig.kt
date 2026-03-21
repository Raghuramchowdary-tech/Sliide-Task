package com.sliide.app.core.data.networking

import kotlin.concurrent.Volatile
import kotlin.jvm.JvmInline

@JvmInline
value class ApiToken(val value: String)

object ApiConfig {
    const val BASE_URL = "https://gorest.co.in/public/v2"
    @Volatile
    var token: ApiToken = ApiToken("")
}
