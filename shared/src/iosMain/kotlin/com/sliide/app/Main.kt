package com.sliide.app

import androidx.compose.ui.window.ComposeUIViewController
import com.sliide.app.core.data.networking.ApiConfig
import com.sliide.app.core.data.networking.ApiToken
import com.sliide.app.core.di.coreModule
import com.sliide.app.features.users.di.usersModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    App()
}

fun initKoin(gorestToken: String) {
    ApiConfig.token = ApiToken(gorestToken)
    startKoin {
        modules(coreModule, usersModule, iosModule)
    }
}
