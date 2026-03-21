package com.sliide.app.core.common

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface TimeProvider {
    fun now(): Instant
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Clock.System.now()
}
