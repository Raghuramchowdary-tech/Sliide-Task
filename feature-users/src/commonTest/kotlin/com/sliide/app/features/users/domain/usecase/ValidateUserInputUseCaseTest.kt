package com.sliide.app.features.users.domain.usecase

import com.sliide.app.core.common.DomainError
import com.sliide.app.core.common.DomainResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ValidateUserInputUseCaseTest {
    private val useCase = ValidateUserInputUseCase()

    @Test
    fun `valid name and email succeeds`() {
        val result = useCase(name = "John Doe", email = "john@example.com")
        assertIs<DomainResult.Success<Unit>>(result)
    }

    @Test
    fun `empty name fails with name violation`() {
        val result = useCase(name = "", email = "john@example.com")
        assertIs<DomainResult.Failure>(result)
        val error = result.error
        assertIs<DomainError.ValidationFailed>(error)
        assertEquals("name", error.violations.first().field)
    }

    @Test
    fun `name shorter than 2 chars fails`() {
        val result = useCase(name = "A", email = "john@example.com")
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `email without correct validation fails with email violation`() {
        val result = useCase(name = "John Doe", email = "johnexample.com")
        assertIs<DomainResult.Failure>(result)
        val error = result.error
        assertIs<DomainError.ValidationFailed>(error)
        assertEquals("email", error.violations.first().field)
    }

    @Test
    fun `email without domain fails`() {
        val result = useCase(name = "John Doe", email = "john@")
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `email without local part fails`() {
        val result = useCase(name = "John Doe", email = "@example.com")
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `blank email fails`() {
        val result = useCase(name = "John Doe", email = "  ")
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `name with only spaces fails`() {
        val result = useCase(name = "   ", email = "john@example.com")
        assertIs<DomainResult.Failure>(result)
    }

    @Test
    fun `both invalid returns two violations`() {
        val result = useCase(name = "", email = "invalid")
        assertIs<DomainResult.Failure>(result)
        val error = result.error
        assertIs<DomainError.ValidationFailed>(error)
        assertEquals(2, error.violations.size)
    }
}
