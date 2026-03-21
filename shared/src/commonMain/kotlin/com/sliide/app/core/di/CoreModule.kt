package com.sliide.app.core.di

import com.sliide.app.core.common.SystemTimeProvider
import com.sliide.app.core.common.TimeProvider
import com.sliide.app.core.data.networking.createHttpClient
import org.koin.dsl.module

val coreModule = module {
    single { createHttpClient() }
    single<TimeProvider> { SystemTimeProvider() }
}
