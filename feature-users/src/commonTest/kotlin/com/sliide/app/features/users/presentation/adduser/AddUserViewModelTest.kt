package com.sliide.app.features.users.presentation.adduser

import app.cash.turbine.test
import com.sliide.app.core.common.DomainError
import com.sliide.app.core.common.DomainResult
import com.sliide.app.core.common.Violation
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.Gender
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.domain.model.UserStatus
import com.sliide.app.features.users.domain.repository.UserRepository
import com.sliide.app.features.users.domain.usecase.AddUserUseCase
import com.sliide.app.features.users.domain.usecase.ValidateUserInputUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddUserViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val createdUser = User(
        id = 10L,
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

    private class FakeRepository(
        var addResult: DomainResult<User> = DomainResult.Success(
            User(10L, "John Doe", "john@example.com", Gender.Male, UserStatus.Active, Instant.parse("2026-03-20T12:00:00Z"))
        ),
    ) : UserRepository {
        override fun observeUsers(): Flow<List<User>> = emptyFlow()
        override suspend fun refresh(): DomainResult<Boolean> = DomainResult.Success(true)
        override suspend fun loadNextPage(): Boolean = false
        override suspend fun addUser(request: CreateUserRequest): DomainResult<User> = addResult
        override suspend fun deleteUser(userId: Long): DomainResult<Unit> = DomainResult.Success(Unit)
        override suspend fun restoreUser(user: User): DomainResult<Unit> = DomainResult.Success(Unit)
    }

    private fun createViewModel(repo: FakeRepository = FakeRepository()): AddUserViewModel {
        val addUserUseCase = AddUserUseCase(repo)
        val validateUseCase = ValidateUserInputUseCase()
        return AddUserViewModel(addUserUseCase, validateUseCase)
    }

    @Test
    fun `submit with valid input sends success effect`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.submit("John Doe", "john@example.com", Gender.Male)

            val effect = awaitItem()
            assertIs<AddUserEffect.Success>(effect)
        }
    }

    @Test
    fun `submit with empty name sets validation errors`() = runTest {
        val vm = createViewModel()

        vm.submit("", "john@example.com", Gender.Male)

        assertTrue(vm.state.value.validationErrors.containsKey("name"))
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `submit with invalid email sets validation errors`() = runTest {
        val vm = createViewModel()

        vm.submit("John Doe", "invalid-email", Gender.Male)

        assertTrue(vm.state.value.validationErrors.containsKey("email"))
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `submit with both invalid sets two validation errors`() = runTest {
        val vm = createViewModel()

        vm.submit("", "bad", Gender.Female)

        assertEquals(2, vm.state.value.validationErrors.size)
        assertTrue(vm.state.value.validationErrors.containsKey("name"))
        assertTrue(vm.state.value.validationErrors.containsKey("email"))
    }

    @Test
    fun `submit clears previous validation errors on valid input`() = runTest {
        val vm = createViewModel()

        // First submit invalid
        vm.submit("", "john@example.com", Gender.Male)
        assertTrue(vm.state.value.validationErrors.isNotEmpty())

        // Then submit valid
        vm.submit("John Doe", "john@example.com", Gender.Male)
        assertTrue(vm.state.value.validationErrors.isEmpty())
    }

    @Test
    fun `submit shows server validation errors on 422`() = runTest {
        val repo = FakeRepository(
            addResult = DomainResult.Failure(
                DomainError.ValidationFailed(
                    listOf(Violation("email", "has already been taken"))
                )
            )
        )
        val vm = createViewModel(repo)

        vm.submit("John Doe", "john@example.com", Gender.Male)

        assertEquals("has already been taken", vm.state.value.validationErrors["email"])
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `submit sends error effect on non-validation failure`() = runTest {
        val repo = FakeRepository(
            addResult = DomainResult.Failure(DomainError.NetworkUnavailable)
        )
        val vm = createViewModel(repo)

        vm.effects.test {
            vm.submit("John Doe", "john@example.com", Gender.Male)

            val effect = awaitItem()
            assertIs<AddUserEffect.Error>(effect)
            assertEquals("Failed to add user", effect.message)
        }

        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `submit trims whitespace from name and email`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.submit("  John Doe  ", "  john@example.com  ", Gender.Male)

            val effect = awaitItem()
            assertIs<AddUserEffect.Success>(effect)
        }
    }
}
