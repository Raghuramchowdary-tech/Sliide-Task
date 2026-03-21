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
        val state = UserListState()
        val next = reducer.reduce(state, UserListResult.Loading)
        assertTrue(next.isLoading)
        assertNull(next.error)
    }

    @Test
    fun `users loaded sets list and clears loading`() {
        val state = UserListState(isLoading = true)
        val next = reducer.reduce(state, UserListResult.UsersLoaded(listOf(testUser)))
        assertEquals(listOf(testUser), next.users)
        assertFalse(next.isLoading)
    }

    @Test
    fun `error sets error and clears loading`() {
        val state = UserListState(isLoading = true)
        val next = reducer.reduce(state, UserListResult.Error(DomainError.NetworkUnavailable))
        assertEquals(DomainError.NetworkUnavailable, next.error)
        assertFalse(next.isLoading)
    }

    @Test
    fun `user deleted removes from list and tracks deleted user`() {
        val state = UserListState(users = listOf(testUser), deleteConfirmUser = testUser)
        val next = reducer.reduce(state, UserListResult.UserDeleted(testUser.id, testUser))
        assertTrue(next.users.isEmpty())
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
    fun `user restored adds back to list sorted by id`() {
        val other = testUser.copy(id = 3L)
        val state = UserListState(users = listOf(other))
        val next = reducer.reduce(state, UserListResult.UserRestored(testUser))
        assertEquals(listOf(other, testUser), next.users)
        assertNull(next.lastDeletedUser)
    }

    @Test
    fun `refreshing sets flag and clears error`() {
        val state = UserListState(error = DomainError.Timeout)
        val next = reducer.reduce(state, UserListResult.Refreshing)
        assertTrue(next.isRefreshing)
        assertNull(next.error)
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

    @Test
    fun `error dismissed clears error`() {
        val state = UserListState(error = DomainError.NetworkUnavailable)
        val next = reducer.reduce(state, UserListResult.ErrorDismissed)
        assertNull(next.error)
    }
}
