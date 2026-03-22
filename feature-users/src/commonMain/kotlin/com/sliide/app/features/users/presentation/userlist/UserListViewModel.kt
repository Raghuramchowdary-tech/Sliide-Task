package com.sliide.app.features.users.presentation.userlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliide.app.core.common.DomainResult
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.repository.UserRepository
import com.sliide.app.features.users.domain.usecase.DeleteUserUseCase
import com.sliide.app.features.users.domain.usecase.GetUsersUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserListViewModel(
    private val getUsers: GetUsersUseCase,
    private val deleteUser: DeleteUserUseCase,
    private val repository: UserRepository,
    private val reducer: UserListReducer,
) : ViewModel() {

    private val _state = MutableStateFlow(UserListState())
    val state: StateFlow<UserListState> = _state.asStateFlow()

    private val _effects = Channel<UserListEffect>(Channel.BUFFERED)
    val effects: Flow<UserListEffect> = _effects.receiveAsFlow()

    init {
        observeUsers()
        loadUsers()
    }

    private fun observeUsers() {
        viewModelScope.launch {
            getUsers().collect { users ->
                emit(UserListResult.UsersLoaded(users))
            }
        }
    }

    private fun loadUsers() {
        emit(UserListResult.Loading)
        viewModelScope.launch {
            when (val result = repository.refresh()) {
                is DomainResult.Success -> emit(UserListResult.PageLoaded(hasMore = result.data))
                is DomainResult.Failure -> emit(UserListResult.Error(result.error))
            }
        }
    }

    private fun refreshUsers() {
        emit(UserListResult.Refreshing)
        viewModelScope.launch {
            when (val result = repository.refresh()) {
                is DomainResult.Success -> emit(UserListResult.PageLoaded(hasMore = result.data))
                is DomainResult.Failure -> emit(UserListResult.Error(result.error))
            }
        }
    }

    private fun loadMore() {
        val s = _state.value
        if (s.isLoadingMore || !s.hasMorePages) return
        emit(UserListResult.LoadingMore)
        viewModelScope.launch {
            val hasMore = repository.loadNextPage()
            emit(UserListResult.PageLoaded(hasMore = hasMore))
        }
    }

    fun onIntent(intent: UserListIntent) {
        when (intent) {
            is UserListIntent.RefreshUsers -> refreshUsers()
            is UserListIntent.LoadMore -> loadMore()
            is UserListIntent.RequestDelete -> emit(UserListResult.DeleteConfirmShown(intent.user))
            is UserListIntent.DismissDeleteConfirm -> emit(UserListResult.DeleteConfirmDismissed)
            is UserListIntent.ConfirmDelete -> confirmDelete()
            is UserListIntent.UndoDelete -> undoDelete()
            is UserListIntent.SelectUser -> emit(UserListResult.UserSelected(intent.user))
            is UserListIntent.DismissError -> emit(UserListResult.ErrorDismissed)
            is UserListIntent.ScrollToTop -> viewModelScope.launch {
                _effects.send(UserListEffect.ScrollToTop)
            }
        }
    }

    private fun confirmDelete() {
        val user = _state.value.deleteConfirmUser ?: return
        emit(UserListResult.UserDeleted(user.id, user))
        viewModelScope.launch {
            _effects.send(UserListEffect.ShowUndoSnackbar(user))
            when (deleteUser(user.id)) {
                is DomainResult.Success -> {}
                is DomainResult.Failure -> {
                    emit(UserListResult.UserRestored(user))
                    _effects.send(UserListEffect.ShowToast("Failed to delete user"))
                }
            }
        }
    }

    private fun undoDelete() {
        val user = _state.value.lastDeletedUser ?: return
        emit(UserListResult.UndoCompleted)
        viewModelScope.launch {
            repository.restoreUser(user)
            _effects.send(UserListEffect.ScrollToUser(user.id))
        }
    }

    private fun emit(result: UserListResult) {
        _state.update { reducer.reduce(it, result) }
    }
}
