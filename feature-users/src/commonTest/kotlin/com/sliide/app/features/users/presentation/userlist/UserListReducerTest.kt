package com.sliide.app.features.users.presentation.userlist

import com.sliide.app.core.common.DomainError
import com.sliide.app.features.users.domain.model.Gender
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.model.UserStatus
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserListReducerTest {
    private val reducer = UserListReducer()
    private val testUser = User(
        id = 1L,
        name = "John Doe",
        email = "john@example.com",
        gender = Gender.Male,
        status = UserStatus.Active,
        createdAt = Instant.parse("2026-03-20T12:00:00Z"),
    )

    @Test
    fun `loading sets isLoading and clears error`() {
        val state = UserListState(error = DomainError.NetworkUnavailable)
        val next = reducer.reduce(state, UserListResult.Loading)
        assertTrue(next.isLoading)
        assertNull(next.error)
    }

    @Test
    fun `refreshing sets isRefreshing and clears error`() {
        val state = UserListState(error = DomainError.NetworkUnavailable)
        val next = reducer.reduce(state, UserListResult.Refreshing)
        assertTrue(next.isRefreshing)
        assertNull(next.error)
    }

    @Test
    fun `loading more sets isLoadingMore`() {
        val state = UserListState()
        val next = reducer.reduce(state, UserListResult.LoadingMore)
        assertTrue(next.isLoadingMore)
    }

    @Test
    fun `page loaded clears loading flags and sets hasMorePages`() {
        val state = UserListState(isLoading = true, isRefreshing = true, isLoadingMore = true)
        val next = reducer.reduce(state, UserListResult.PageLoaded(hasMore = false))
        assertFalse(next.isLoading)
        assertFalse(next.isRefreshing)
        assertFalse(next.isLoadingMore)
        assertFalse(next.hasMorePages)
    }

    @Test
    fun `users loaded updates user list without clearing loading`() {
        val users = listOf(testUser)
        val state = UserListState(isLoading = true)
        val next = reducer.reduce(state, UserListResult.UsersLoaded(users))
        assertEquals(users, next.users)
        assertTrue(next.isLoading)
    }

    @Test
    fun `error sets error state and clears loading flags`() {
        val state = UserListState(isLoading = true)
        val next = reducer.reduce(state, UserListResult.Error(DomainError.NetworkUnavailable))
        assertEquals(DomainError.NetworkUnavailable, next.error)
        assertFalse(next.isLoading)
        assertFalse(next.isRefreshing)
        assertFalse(next.isLoadingMore)
    }

    @Test
    fun `error dismissed clears error`() {
        val state = UserListState(error = DomainError.NetworkUnavailable)
        val next = reducer.reduce(state, UserListResult.ErrorDismissed)
        assertNull(next.error)
    }

    @Test
    fun `user deleted tracks deleted user and clears confirm`() {
        val state = UserListState(deleteConfirmUser = testUser)
        val next = reducer.reduce(state, UserListResult.UserDeleted(testUser.id, testUser))
        assertNull(next.deleteConfirmUser)
        assertEquals(testUser, next.lastDeletedUser)
    }

    @Test
    fun `undo completed clears last deleted user`() {
        val state = UserListState(lastDeletedUser = testUser)
        val next = reducer.reduce(state, UserListResult.UndoCompleted)
        assertNull(next.lastDeletedUser)
    }

    @Test
    fun `user restored clears last deleted user`() {
        val state = UserListState(lastDeletedUser = testUser)
        val next = reducer.reduce(state, UserListResult.UserRestored(testUser))
        assertNull(next.lastDeletedUser)
    }

    @Test
    fun `delete confirm shown sets user`() {
        val state = UserListState()
        val next = reducer.reduce(state, UserListResult.DeleteConfirmShown(testUser))
        assertEquals(testUser, next.deleteConfirmUser)
    }

    @Test
    fun `select user sets selected`() {
        val state = UserListState()
        val next = reducer.reduce(state, UserListResult.UserSelected(testUser))
        assertEquals(testUser, next.selectedUser)
    }
}
