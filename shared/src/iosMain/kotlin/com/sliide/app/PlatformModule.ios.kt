package com.sliide.app

import com.sliide.app.core.data.persistence.AppDatabase
import com.sliide.app.core.data.persistence.buildDatabase
import com.sliide.app.core.data.persistence.getDatabaseBuilder
import org.koin.dsl.module

val iosModule = module {
    single<AppDatabase> { getDatabaseBuilder().buildDatabase() }
}
