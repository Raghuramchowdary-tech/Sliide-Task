package com.sliide.app.features.users.presentation.userlist

import com.sliide.app.core.common.DomainError
import com.sliide.app.features.users.domain.model.User

data class UserListState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: DomainError? = null,
    val deleteConfirmUser: User? = null,
    val lastDeletedUser: User? = null,
    val selectedUser: User? = null,
)

sealed interface UserListIntent {
    data object LoadUsers : UserListIntent
    data object RefreshUsers : UserListIntent
    data class RequestDelete(val user: User) : UserListIntent
    data object DismissDeleteConfirm : UserListIntent
    data object ConfirmDelete : UserListIntent
    data object UndoDelete : UserListIntent
    data class SelectUser(val user: User) : UserListIntent
    data object DismissError : UserListIntent
    data object ScrollToTop : UserListIntent
}

sealed interface UserListResult {
    data object Loading : UserListResult
    data object Refreshing : UserListResult
    data class UsersLoaded(val users: List<User>) : UserListResult
    data class Error(val error: DomainError) : UserListResult
    data object ErrorDismissed : UserListResult
    data class DeleteConfirmShown(val user: User) : UserListResult
    data object DeleteConfirmDismissed : UserListResult
    data class UserDeleted(val userId: Long, val deletedUser: User) : UserListResult
    data object UndoCompleted : UserListResult
    data class UserRestored(val user: User) : UserListResult
    data class UserSelected(val user: User) : UserListResult
}

sealed interface UserListEffect {
    data class ShowUndoSnackbar(val deletedUser: User) : UserListEffect
    data class ShowToast(val message: String) : UserListEffect
    data class ScrollToUser(val userId: Long) : UserListEffect
    data object ScrollToTop : UserListEffect
}
