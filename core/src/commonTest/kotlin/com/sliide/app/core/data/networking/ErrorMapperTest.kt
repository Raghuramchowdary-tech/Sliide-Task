package com.sliide.app.core.data.networking

import com.sliide.app.core.common.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ErrorMapperTest {

    // -- toHttpDomainError ------------------------------------------------

    @Test
    fun `401 maps to Unauthorized`() {
        assertIs<DomainError.Unauthorized>(401.toHttpDomainError())
    }

    @Test
    fun `404 maps to NotFound`() {
        assertIs<DomainError.NotFound>(404.toHttpDomainError())
    }

    @Test
    fun `429 maps to RateLimited`() {
        assertIs<DomainError.RateLimited>(429.toHttpDomainError())
    }

    @Test
    fun `500 maps to ServerError with code 500`() {
        val error = 500.toHttpDomainError()
        assertIs<DomainError.ServerError>(error)
        assertEquals(500, error.code)
        assertEquals("Server error", error.message)
    }

    @Test
    fun `503 maps to ServerError with code 503`() {
        val error = 503.toHttpDomainError()
        assertIs<DomainError.ServerError>(error)
        assertEquals(503, error.code)
        assertEquals("Server error", error.message)
    }

    @Test
    fun `200 maps to Unknown`() {
        assertIs<DomainError.Unknown>(200.toHttpDomainError())
    }

    @Test
    fun `0 maps to Unknown`() {
        assertIs<DomainError.Unknown>(0.toHttpDomainError())
    }

    // -- toDomainError ----------------------------------------------------

    @Test
    fun `RuntimeException maps to Unknown`() {
        val exception = RuntimeException("boom")
        val error = exception.toDomainError()
        assertIs<DomainError.Unknown>(error)
        assertEquals(exception, error.cause)
    }

    @Test
    fun `IllegalStateException maps to Unknown`() {
        val exception = IllegalStateException("bad state")
        val error = exception.toDomainError()
        assertIs<DomainError.Unknown>(error)
        assertEquals(exception, error.cause)
    }
}
