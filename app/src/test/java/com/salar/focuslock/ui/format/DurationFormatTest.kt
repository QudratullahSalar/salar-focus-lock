package com.salar.focuslock.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.util.Locale

/**
 * Unit tests for the pure formatting helpers. Written but NOT executed — no JVM available in
 * this environment; verified by manual read-through only.
 */
class DurationFormatTest {

    @Test
    fun `zero duration formats as 00-00-00`() {
        assertEquals("00:00:00", Duration.ZERO.toClockString())
    }

    @Test
    fun `sub-minute duration formats correctly`() {
        assertEquals("00:00:45", Duration.ofSeconds(45).toClockString())
    }

    @Test
    fun `matches the spec's example duration (1h 42m 18s)`() {
        val duration = Duration.ofHours(1).plusMinutes(42).plusSeconds(18)
        assertEquals("01:42:18", duration.toClockString())
    }

    @Test
    fun `exactly one hour formats as 01-00-00`() {
        assertEquals("01:00:00", Duration.ofHours(1).toClockString())
    }

    @Test
    fun `negative duration (defensive) does not produce a negative or malformed string`() {
        // ScheduleEngine never actually hands BlockingEngine/the calculator a negative
        // Duration (getRemainingDuration coerces to zero), but toClockString defends anyway.
        assertEquals("00:00:00", Duration.ofSeconds(-5).toClockString())
    }

    @Test
    fun `unlock time formats as the spec's example, using an explicit locale for determinism`() {
        assertEquals("10:00 AM", LocalTime.of(10, 0).toUnlockTimeString(Locale.US))
    }

    @Test
    fun `unlock time handles noon and midnight correctly`() {
        assertEquals("12:00 PM", LocalTime.NOON.toUnlockTimeString(Locale.US))
        assertEquals("12:00 AM", LocalTime.MIDNIGHT.toUnlockTimeString(Locale.US))
    }
}
