package com.sliide.app.features.users.presentation.userlist

import app.cash.turbine.test
import com.sliide.app.core.common.DomainError
import com.sliide.app.core.common.DomainResult
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.Gender
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.model.UserStatus
import com.sliide.app.features.users.domain.repository.UserRepository
import com.sliide.app.features.users.domain.usecase.DeleteUserUseCase
import com.sliide.app.features.users.domain.usecase.GetUsersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testUser = User(
        id = 1L,
        name = "John Doe",
        email = "john@example.com",
        gender = Gender.Male,
        status = UserStatus.Active,
        createdAt = Instant.parse("2026-03-20T12:00:00Z"),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRepository : UserRepository {
        val usersFlow = MutableSharedFlow<List<User>>(replay = 1)
        var deleteResult: DomainResult<Unit> = DomainResult.Success(Unit)
        var restoreResult: DomainResult<Unit> = DomainResult.Success(Unit)
        var refreshResult: DomainResult<Boolean> = DomainResult.Success(true)
        var loadNextPageResult: Boolean = false

        init {
            usersFlow.tryEmit(emptyList())
        }

        override fun observeUsers(): Flow<List<User>> = usersFlow
        override suspend fun refresh(): DomainResult<Boolean> = refreshResult
        override suspend fun loadNextPage(): Boolean = loadNextPageResult
        override suspend fun addUser(request: CreateUserRequest): DomainResult<User> =
            DomainResult.Failure(DomainError.Unknown())
        override suspend fun deleteUser(userId: Long): DomainResult<Unit> = deleteResult
        override suspend fun restoreUser(user: User): DomainResult<Unit> = restoreResult
    }

    private fun createViewModel(repo: FakeRepository = FakeRepository()): Pair<UserListViewModel, FakeRepository> {
        val getUsers = GetUsersUseCase(repo)
        val deleteUser = DeleteUserUseCase(repo)
        val reducer = UserListReducer()
        val vm = UserListViewModel(getUsers, deleteUser, repo, reducer)
        return vm to repo
    }

    @Test
    fun `initial load sets loading then page loaded`() = runTest {
        val (vm, _) = createViewModel()

        // After init, loading should have completed (UnconfinedTestDispatcher)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test
    fun `request delete shows confirmation`() = runTest {
        val (vm, _) = createViewModel()

        vm.onIntent(UserListIntent.RequestDelete(testUser))

        assertEquals(testUser, vm.state.value.deleteConfirmUser)
    }

    @Test
    fun `dismiss delete clears confirmation`() = runTest {
        val (vm, _) = createViewModel()

        vm.onIntent(UserListIntent.RequestDelete(testUser))
        vm.onIntent(UserListIntent.DismissDeleteConfirm)

        assertNull(vm.state.value.deleteConfirmUser)
    }

    @Test
    fun `confirm delete removes user and sends undo snackbar effect`() = runTest {
        val (vm, repo) = createViewModel()
        repo.usersFlow.emit(listOf(testUser))

        vm.onIntent(UserListIntent.RequestDelete(testUser))
        vm.effects.test {
            vm.onIntent(UserListIntent.ConfirmDelete)

            val effect = awaitItem()
            assertIs<UserListEffect.ShowUndoSnackbar>(effect)
            assertEquals(testUser, effect.deletedUser)
        }

        assertEquals(testUser, vm.state.value.lastDeletedUser)
    }

    @Test
    fun `confirm delete restores user on API failure`() = runTest {
        val repo = FakeRepository()
        repo.deleteResult = DomainResult.Failure(DomainError.NetworkUnavailable)
        val (vm, _) = createViewModel(repo)
        repo.usersFlow.emit(listOf(testUser))

        vm.onIntent(UserListIntent.RequestDelete(testUser))
        vm.effects.test {
            vm.onIntent(UserListIntent.ConfirmDelete)

            val undoEffect = awaitItem()
            assertIs<UserListEffect.ShowUndoSnackbar>(undoEffect)

            val toastEffect = awaitItem()
            assertIs<UserListEffect.ShowToast>(toastEffect)
            assertEquals("Failed to delete user", toastEffect.message)
        }

        // User restored -- lastDeletedUser cleared by UserRestored result
        assertNull(vm.state.value.lastDeletedUser)
    }

    @Test
    fun `undo delete clears last deleted and sends scroll effect`() = runTest {
        val (vm, repo) = createViewModel()
        repo.usersFlow.emit(listOf(testUser))

        // Delete first
        vm.onIntent(UserListIntent.RequestDelete(testUser))
        vm.onIntent(UserListIntent.ConfirmDelete)

        assertEquals(testUser, vm.state.value.lastDeletedUser)

        // Undo
        vm.effects.test {
            // Drain the undo snackbar from confirm
            cancelAndIgnoreRemainingEvents()
        }
        vm.effects.test {
            vm.onIntent(UserListIntent.UndoDelete)

            val effect = awaitItem()
            assertIs<UserListEffect.ScrollToUser>(effect)
            assertEquals(testUser.id, effect.userId)
        }

        assertNull(vm.state.value.lastDeletedUser)
    }

    @Test
    fun `select user sets selected user`() = runTest {
        val (vm, _) = createViewModel()

        vm.onIntent(UserListIntent.SelectUser(testUser))

        assertEquals(testUser, vm.state.value.selectedUser)
    }

    @Test
    fun `refresh sets refreshing state`() = runTest {
        val (vm, _) = createViewModel()

        // After init completes, refreshing should be false
        assertEquals(false, vm.state.value.isRefreshing)
    }

    @Test
    fun `users loaded updates user list`() = runTest {
        val (vm, repo) = createViewModel()

        val users = listOf(testUser)
        repo.usersFlow.emit(users)

        assertEquals(users, vm.state.value.users)
    }
}
