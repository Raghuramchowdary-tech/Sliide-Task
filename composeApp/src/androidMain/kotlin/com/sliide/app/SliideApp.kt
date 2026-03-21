package com.sliide.app

import android.app.Application
import com.sliide.app.core.data.networking.ApiConfig
import com.sliide.app.core.data.networking.ApiToken
import com.sliide.app.core.di.coreModule
import com.sliide.app.features.users.di.usersModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SliideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiConfig.token = ApiToken(BuildConfig.GOREST_TOKEN)
        startKoin {
            androidContext(this@SliideApp)
            modules(coreModule, usersModule, androidModule)
        }
    }
}
