package com.salar.focuslock.ui.format

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats a Duration as HH:MM:SS for the Focus Lock countdown (matching the spec's example,
 * "01:42:18"). Pure Kotlin — no Android dependency — so it's directly unit-testable without
 * Robolectric or an instrumented device.
 */
fun Duration.toClockString(): String {
    val totalSeconds = seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, secs)
}

/**
 * Formats a LocalTime as "10:00 AM" (matching the spec's example). [locale] defaults to the
 * device's current locale for production use, but is an explicit parameter (rather than
 * baked in) specifically so a unit test can pass a fixed locale (e.g. Locale.US) and assert on
 * exact AM/PM text deterministically — relying on the JVM's default locale in a test would be
 * flaky, since it depends on the environment running the test, not the code under test. This
 * also matters for real use: Salar Focus Lock targets Pashto/Dari-speaking users, where the
 * device's default locale (and therefore this string's script/format) may not be English.
 */
fun LocalTime.toUnlockTimeString(locale: Locale = Locale.getDefault()): String =
    format(DateTimeFormatter.ofPattern("hh:mm a", locale))
