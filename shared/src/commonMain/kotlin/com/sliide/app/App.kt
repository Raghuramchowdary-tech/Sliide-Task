package com.sliide.app

import androidx.compose.runtime.Composable
import com.sliide.app.core.ui.theme.SliideTheme
import com.sliide.app.features.users.presentation.userlist.ui.UserListScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        SliideTheme {
            UserListScreen()
        }
    }
}
