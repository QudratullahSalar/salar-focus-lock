package com.salar.focuslock.domain.blocking

import com.salar.focuslock.domain.model.FocusRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests for FocusLockScreenStateCalculator, the pure logic behind the Focus Lock screen's
 * remaining-time display. Written but NOT executed — no JVM available in this environment;
 * verified by manual read-through against the calculator's and ScheduleEngine's logic
 * (ScheduleEngine's own behavior was independently verified this same way in Batch 1).
 */
class FocusLockScreenStateCalculatorTest {

    private val everyDay: Set<DayOfWeek> = DayOfWeek.values().toSet()

    private fun dt(day: Int, hour: Int, minute: Int): LocalDateTime =
        LocalDateTime.of(2024, 1, day, hour, minute)

    private fun rule(
        id: Long = 1,
        name: String = "Morning Focus",
        days: Set<DayOfWeek> = everyDay,
        start: Int = 7 * 60,
        end: Int = 10 * 60,
        enabled: Boolean = true
    ): FocusRule = FocusRule(
        id = id,
        name = name,
        startMinuteOfDay = start,
        endMinuteOfDay = end,
        repeatDays = days,
        isEnabled = enabled
    )

    @Test
    fun `active rule produces correct remaining time, unlock time, and isStillActive`() {
        val r = rule(start = 7 * 60, end = 10 * 60) // 07:00 -> 10:00

        val state = FocusLockScreenStateCalculator.calculate(
            rule = r,
            packageName = "com.example.tiktok",
            appName = "TikTok",
            currentDateTime = dt(1, 9, 0) // one hour before end
        )

        requireNotNull(state)
        assertTrue(state.isStillActive)
        assertEquals(Duration.ofHours(1), state.remainingDuration)
        assertEquals(LocalTime.of(10, 0), state.unlockAt)
        assertEquals("com.example.tiktok", state.packageName)
        assertEquals("TikTok", state.appName)
        assertEquals("Morning Focus", state.ruleName)
    }

    @Test
    fun `null rule (deleted or unresolvable) returns null rather than throwing`() {
        val state = FocusLockScreenStateCalculator.calculate(
            rule = null,
            packageName = "com.example.tiktok",
            appName = "TikTok",
            currentDateTime = dt(1, 9, 0)
        )
        assertNull(state)
    }

    @Test
    fun `rule whose window has already ended reports isStillActive false`() {
        val r = rule(start = 7 * 60, end = 10 * 60)

        val state = FocusLockScreenStateCalculator.calculate(
            rule = r,
            packageName = "com.example.tiktok",
            appName = "TikTok",
            currentDateTime = dt(1, 12, 0) // noon, well after 10:00
        )

        requireNotNull(state)
        assertFalse(state.isStillActive)
        assertEquals(Duration.ZERO, state.remainingDuration)
    }

    @Test
    fun `disabled rule reports isStillActive false even inside its nominal time window`() {
        val r = rule(start = 7 * 60, end = 10 * 60, enabled = false)

        val state = FocusLockScreenStateCalculator.calculate(
            rule = r,
            packageName = "com.example.tiktok",
            appName = "TikTok",
            currentDateTime = dt(1, 8, 0)
        )

        requireNotNull(state)
        assertFalse(state.isStillActive)
    }

    @Test
    fun `overnight rule reuses ScheduleEngine correctly across the midnight boundary`() {
        // Same overnight semantics already verified in ScheduleEngineTest — this just confirms
        // the calculator wires them through correctly, not re-deriving them.
        val r = rule(name = "Night Focus", days = setOf(DayOfWeek.MONDAY), start = 23 * 60, end = 60)

        val state = FocusLockScreenStateCalculator.calculate(
            rule = r,
            packageName = "com.example.game",
            appName = "Some Game",
            currentDateTime = dt(2, 0, 30) // 00:30 Tuesday, still within the Monday-started window
        )

        requireNotNull(state)
        assertTrue(state.isStillActive)
        assertEquals(Duration.ofMinutes(30), state.remainingDuration)
        assertEquals(LocalTime.of(1, 0), state.unlockAt)
    }

    @Test
    fun `whatever appName string is passed through is used as-is, including a raw package name fallback`() {
        // Simulates the FocusLockRoute fallback for an app whose label couldn't be resolved
        // (e.g. uninstalled between detection and display) — this layer doesn't care what the
        // string is, it just carries it through, so that fallback is handled safely by
        // construction rather than needing special-casing here.
        val r = rule()
        val state = FocusLockScreenStateCalculator.calculate(
            rule = r,
            packageName = "com.example.vanished",
            appName = "com.example.vanished", // fallback value equal to the raw package name
            currentDateTime = dt(1, 8, 0)
        )

        requireNotNull(state)
        assertEquals("com.example.vanished", state.appName)
    }

    @Test
    fun `only the given rule id's rule is ever considered — no re-selection among multiple rules`() {
        // FocusLockScreenStateCalculator.calculate takes a single, already-resolved FocusRule —
        // there is no list-of-rules parameter for it to re-select from, so BlockingEngine's
        // deterministic tie-break (tested in BlockingEngineTest) cannot be duplicated or
        // diverged from here by construction. This test simply documents/locks in that shape.
        val chosenRule = rule(id = 2, name = "Narrow", start = 9 * 60, end = 9 * 60 + 30)

        val state = FocusLockScreenStateCalculator.calculate(
            rule = chosenRule,
            packageName = "com.example.tiktok",
            appName = "TikTok",
            currentDateTime = dt(1, 9, 15)
        )

        requireNotNull(state)
        assertEquals("Narrow", state.ruleName)
    }
}
