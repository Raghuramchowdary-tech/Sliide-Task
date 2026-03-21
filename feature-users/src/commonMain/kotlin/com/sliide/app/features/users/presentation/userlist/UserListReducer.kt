package com.sliide.app.features.users.presentation.userlist

class UserListReducer {
    fun reduce(state: UserListState, result: UserListResult): UserListState = when (result) {
        is UserListResult.Loading -> state.copy(isLoading = true, error = null)
        is UserListResult.Refreshing -> state.copy(isRefreshing = true, error = null)
        is UserListResult.UsersLoaded -> state.copy(users = result.users, isLoading = false, isRefreshing = false)
        is UserListResult.Error -> state.copy(error = result.error, isLoading = false, isRefreshing = false)
        is UserListResult.ErrorDismissed -> state.copy(error = null)
        is UserListResult.DeleteConfirmShown -> state.copy(deleteConfirmUser = result.user)
        is UserListResult.DeleteConfirmDismissed -> state.copy(deleteConfirmUser = null)
        is UserListResult.UserDeleted -> state.copy(
            users = state.users.filter { it.id != result.userId },
            deleteConfirmUser = null,
            lastDeletedUser = result.deletedUser,
        )
        is UserListResult.UndoCompleted -> state.copy(lastDeletedUser = null)
        is UserListResult.UserRestored -> state.copy(
            users = (state.users + result.user).sortedByDescending { it.id },
            lastDeletedUser = null,
        )
        is UserListResult.UserSelected -> state.copy(selectedUser = result.user)
    }
}
