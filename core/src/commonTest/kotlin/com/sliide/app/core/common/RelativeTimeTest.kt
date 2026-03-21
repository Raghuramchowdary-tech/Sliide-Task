package com.sliide.app.core.common

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RelativeTimeTest {
    private val fixedNow = Instant.parse("2026-03-20T12:00:00Z")
    private val timeProvider = object : TimeProvider {
        override fun now(): Instant = fixedNow
    }

    @Test
    fun `under 60 seconds shows just now`() {
        assertEquals("Just now", RelativeTime.format(fixedNow - 5.seconds, timeProvider))
    }

    @Test
    fun `1 minute shows singular`() {
        assertEquals("1 minute ago", RelativeTime.format(fixedNow - 1.minutes, timeProvider))
    }

    @Test
    fun `5 minutes shows plural`() {
        assertEquals("5 minutes ago", RelativeTime.format(fixedNow - 5.minutes, timeProvider))
    }

    @Test
    fun `1 hour shows singular`() {
        assertEquals("1 hour ago", RelativeTime.format(fixedNow - 1.hours, timeProvider))
    }

    @Test
    fun `3 hours shows plural`() {
        assertEquals("3 hours ago", RelativeTime.format(fixedNow - 3.hours, timeProvider))
    }

    @Test
    fun `1 day shows singular`() {
        assertEquals("1 day ago", RelativeTime.format(fixedNow - 1.days, timeProvider))
    }

    @Test
    fun `2 days shows plural`() {
        assertEquals("2 days ago", RelativeTime.format(fixedNow - 2.days, timeProvider))
    }

    @Test
    fun `14 days shows 2 weeks`() {
        assertEquals("2 weeks ago", RelativeTime.format(fixedNow - 14.days, timeProvider))
    }

    @Test
    fun `60 days shows 2 months`() {
        assertEquals("2 months ago", RelativeTime.format(fixedNow - 60.days, timeProvider))
    }
}
