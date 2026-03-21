package com.sliide.app.features.users.di

import com.sliide.app.core.data.persistence.AppDatabase
import com.sliide.app.features.users.data.mapper.UserMapper
import com.sliide.app.features.users.data.remote.UserRemoteDataSource
import com.sliide.app.features.users.data.repository.UserRepositoryImpl
import com.sliide.app.features.users.domain.repository.UserRepository
import com.sliide.app.features.users.domain.usecase.AddUserUseCase
import com.sliide.app.features.users.domain.usecase.DeleteUserUseCase
import com.sliide.app.features.users.domain.usecase.GetUsersUseCase
import com.sliide.app.features.users.domain.usecase.ValidateUserInputUseCase
import com.sliide.app.features.users.presentation.adduser.AddUserViewModel
import com.sliide.app.features.users.presentation.userlist.UserListReducer
import com.sliide.app.features.users.presentation.userlist.UserListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val usersModule = module {
    factory { UserRemoteDataSource(get()) }
    factory { UserMapper() }
    factory { get<AppDatabase>().userDao() }
    factory { get<AppDatabase>().pendingDeleteDao() }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get()) }

    factory { GetUsersUseCase(get()) }
    factory { AddUserUseCase(get()) }
    factory { DeleteUserUseCase(get()) }
    factory { ValidateUserInputUseCase() }

    factory { UserListReducer() }
    viewModel { UserListViewModel(get(), get(), get(), get()) }
    viewModel { AddUserViewModel(get(), get()) }
}
