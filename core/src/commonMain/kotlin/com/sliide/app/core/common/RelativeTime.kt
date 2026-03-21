package com.sliide.app.core.common

import kotlinx.datetime.Instant

object RelativeTime {
    fun format(timestamp: Instant, timeProvider: TimeProvider): String {
        val now = timeProvider.now()
        val diff = now - timestamp
        val seconds = diff.inWholeSeconds
        val minutes = diff.inWholeMinutes
        val hours = diff.inWholeHours
        val days = diff.inWholeDays
        val weeks = days / 7
        val months = days / 30

        return when {
            seconds < 60 -> "Just now"
            minutes == 1L -> "1 minute ago"
            minutes < 60 -> "$minutes minutes ago"
            hours == 1L -> "1 hour ago"
            hours < 24 -> "$hours hours ago"
            days == 1L -> "1 day ago"
            days < 7 -> "$days days ago"
            weeks == 1L -> "1 week ago"
            days < 30 -> "$weeks weeks ago"
            months == 1L -> "1 month ago"
            else -> "$months months ago"
        }
    }
}
