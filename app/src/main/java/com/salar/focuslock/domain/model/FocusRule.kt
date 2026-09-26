package com.salar.focuslock.domain.model

import java.time.DayOfWeek
import java.time.Instant

/**
 * A user-defined focus schedule: a time-of-day window, repeated on selected weekdays,
 * during which the associated [BlockedApp]s should be locked.
 *
 * Times are stored as plain minute-of-day integers (0..1439) rather than [java.time.LocalTime]
 * so Room persistence stays trivial (two Ints, no TypeConverter). All values here are assumed
 * to already be in the device's local wall-clock time — see [com.salar.focuslock.domain.scheduler.ScheduleEngine]
 * for how the current-time comparisons are expected to be supplied.
 *
 * [endMinuteOfDay] may be less than [startMinuteOfDay] to express an overnight window,
 * e.g. startMinuteOfDay = 1380 (23:00), endMinuteOfDay = 60 (01:00).
 */
data class FocusRule(
    val id: Long = 0L,
    val name: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val repeatDays: Set<DayOfWeek>,
    val isEnabled: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(startMinuteOfDay in 0..1439) {
            "startMinuteOfDay must be within 0..1439, was $startMinuteOfDay"
        }
        require(endMinuteOfDay in 0..1439) {
            "endMinuteOfDay must be within 0..1439, was $endMinuteOfDay"
        }
    }

    /**
     * True when the window crosses midnight (e.g. 23:00 -> 01:00).
     * A rule where start == end is a degenerate zero-duration window, not an overnight one —
     * see [durationMinutes].
     */
    val isOvernight: Boolean get() = endMinuteOfDay < startMinuteOfDay

    /**
     * Total minutes the window spans. Zero when startMinuteOfDay == endMinuteOfDay, which is
     * treated as an invalid/degenerate rule by [com.salar.focuslock.domain.scheduler.ScheduleEngine]
     * (never active) rather than as a 24-hour window — UI-level validation in a later batch
     * should prevent creating such a rule in the first place.
     */
    val durationMinutes: Int
        get() = when {
            endMinuteOfDay > startMinuteOfDay -> endMinuteOfDay - startMinuteOfDay
            endMinuteOfDay < startMinuteOfDay -> (MINUTES_PER_DAY - startMinuteOfDay) + endMinuteOfDay
            else -> 0
        }

    companion object {
        const val MINUTES_PER_DAY = 1440
    }
}
