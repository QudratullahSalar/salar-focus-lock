package com.salar.focuslock.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.util.Locale

/** Written but NOT executed locally (no JVM/Gradle in the authoring environment). */
class RuleFormatTest {

    @Test
    fun `formatMinuteOfDay formats morning, midnight and afternoon`() {
        assertEquals("07:00 AM", formatMinuteOfDay(7 * 60, Locale.US))
        assertEquals("12:00 AM", formatMinuteOfDay(0, Locale.US))
        assertEquals("01:05 PM", formatMinuteOfDay(13 * 60 + 5, Locale.US))
    }

    @Test
    fun `formatMinuteOfDay clamps out of range input instead of throwing`() {
        assertEquals("11:59 PM", formatMinuteOfDay(5000, Locale.US))
        assertEquals("12:00 AM", formatMinuteOfDay(-10, Locale.US))
    }

    @Test
    fun `formatTimeRange joins start and end`() {
        assertEquals("07:00 AM – 10:00 AM", formatTimeRange(7 * 60, 10 * 60, Locale.US))
    }

    @Test
    fun `summarizeDays recognises every day, weekdays and weekends`() {
        assertEquals("Every day", summarizeDays(DayOfWeek.values().toSet(), Locale.US))
        assertEquals(
            "Weekdays",
            summarizeDays(
                setOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                ),
                Locale.US
            )
        )
        assertEquals("Weekends", summarizeDays(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), Locale.US))
    }

    @Test
    fun `summarizeDays lists custom selections in Monday-first order`() {
        val days = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        assertEquals("Mon, Wed, Fri", summarizeDays(days, Locale.US))
    }

    @Test
    fun `summarizeDays handles empty selection`() {
        assertEquals("No days", summarizeDays(emptySet(), Locale.US))
    }

    @Test
    fun `summarizeApps handles none, few and many`() {
        assertEquals("No apps", summarizeApps(emptyList()))
        assertEquals("TikTok, YouTube", summarizeApps(listOf("TikTok", "YouTube")))
        assertEquals("A, B, C +2 more", summarizeApps(listOf("A", "B", "C", "D", "E")))
    }
}
