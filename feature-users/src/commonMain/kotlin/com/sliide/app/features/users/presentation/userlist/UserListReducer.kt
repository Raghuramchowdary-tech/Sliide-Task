package com.sliide.app.features.users.presentation.userlist

class UserListReducer {
    fun reduce(state: UserListState, result: UserListResult): UserListState = when (result) {
        is UserListResult.Loading -> state.copy(isLoading = true, error = null)
        is UserListResult.Refreshing -> state.copy(isRefreshing = true, error = null)
        is UserListResult.LoadingMore -> state.copy(isLoadingMore = true)
        is UserListResult.PageLoaded -> state.copy(
            isLoading = false,
            isRefreshing = false,
            isLoadingMore = false,
            hasMorePages = result.hasMore,
        )
        is UserListResult.UsersLoaded -> state.copy(
            users = result.users,
        )
        is UserListResult.Error -> state.copy(
            error = result.error,
            isLoading = false,
            isRefreshing = false,
            isLoadingMore = false,
        )
        is UserListResult.ErrorDismissed -> state.copy(error = null)
        is UserListResult.DeleteConfirmShown -> state.copy(deleteConfirmUser = result.user)
        is UserListResult.DeleteConfirmDismissed -> state.copy(deleteConfirmUser = null)
        is UserListResult.UserDeleted -> state.copy(
            deleteConfirmUser = null,
            lastDeletedUser = result.deletedUser,
        )
        is UserListResult.UndoCompleted -> state.copy(lastDeletedUser = null)
        is UserListResult.UserRestored -> state.copy(lastDeletedUser = null)
        is UserListResult.UserSelected -> state.copy(selectedUser = result.user)
    }
}
