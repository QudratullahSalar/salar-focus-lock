package com.salar.focuslock.ui.format

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** 0..1439 minute-of-day -> "07:00 AM" (values outside the range are clamped). */
fun formatMinuteOfDay(minuteOfDay: Int, locale: Locale = Locale.getDefault()): String {
    val m = minuteOfDay.coerceIn(0, 1439)
    return LocalTime.of(m / 60, m % 60).toUnlockTimeString(locale)
}

fun formatTimeRange(startMinute: Int, endMinute: Int, locale: Locale = Locale.getDefault()): String =
    "${formatMinuteOfDay(startMinute, locale)} – ${formatMinuteOfDay(endMinute, locale)}"

fun summarizeDays(days: Set<DayOfWeek>, locale: Locale = Locale.getDefault()): String {
    val weekdays = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    )
    val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    return when {
        days.isEmpty() -> "No days"
        days == DayOfWeek.values().toSet() -> "Every day"
        days == weekdays -> "Weekdays"
        days == weekend -> "Weekends"
        else -> DayOfWeek.values().filter { it in days }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
}

fun summarizeApps(labels: List<String>, maxShown: Int = 3): String = when {
    labels.isEmpty() -> "No apps"
    labels.size <= maxShown -> labels.joinToString(", ")
    else -> labels.take(maxShown).joinToString(", ") + " +${labels.size - maxShown} more"
}
