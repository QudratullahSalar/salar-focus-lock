package com.salar.focuslock.domain.scheduler

import com.salar.focuslock.domain.model.FocusRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

/**
 * Unit tests for ScheduleEngine. Pure JVM tests (java.time + JUnit4 only, no Android
 * framework, no Robolectric) so they are intended to run under `./gradlew testDebugUnitTest`
 * once a real build environment is available.
 *
 * IMPORTANT: these tests were written but have NOT been executed — there is no JVM/Gradle
 * available in this environment to run them. They were checked by static/manual read-through
 * against ScheduleEngine's logic only. Treat pass/fail as unverified until run for real.
 *
 * All fixture dates below use a fixed reference week (Mon 2024-01-01 .. Sun 2024-01-07) so
 * dayOfWeek arithmetic is unambiguous:
 *   2024-01-01 = Monday, 01-02 = Tuesday, 01-03 = Wednesday, 01-04 = Thursday,
 *   01-05 = Friday, 01-06 = Saturday, 01-07 = Sunday.
 */
class ScheduleEngineTest {

    // DayOfWeek is a java.time (Java-declared) enum, so Kotlin's synthetic `entries` property
    // is not available here — use values().
    private val everyDay: Set<DayOfWeek> = DayOfWeek.values().toSet()

    private fun dt(day: Int, hour: Int, minute: Int, second: Int = 0): LocalDateTime =
        LocalDateTime.of(2024, 1, day, hour, minute, second)

    // ---- Same-day window (07:00 -> 10:00) -----------------------------------------

    @Test
    fun `same-day window is inactive before start`() {
        val rule = sameDayRule()
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(1, 6, 59, 59)))
    }

    @Test
    fun `same-day window is active at exact start time`() {
        val rule = sameDayRule()
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(1, 7, 0, 0)))
    }

    @Test
    fun `same-day window is active in the middle`() {
        val rule = sameDayRule()
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(1, 8, 30)))
    }

    @Test
    fun `same-day window is active one second before end`() {
        val rule = sameDayRule()
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(1, 9, 59, 59)))
    }

    @Test
    fun `same-day window is inactive at exact end time (end is exclusive)`() {
        val rule = sameDayRule()
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(1, 10, 0, 0)))
    }

    @Test
    fun `same-day window is inactive after end`() {
        val rule = sameDayRule()
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(1, 10, 0, 1)))
    }

    // ---- Overnight window (23:00 -> 01:00), midnight crossing ---------------------

    @Test
    fun `overnight window is inactive before start`() {
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(1, 22, 59, 59)))
    }

    @Test
    fun `overnight window is active at exact start time on the starting day`() {
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(1, 23, 0, 0)))
    }

    @Test
    fun `overnight window is active just before midnight`() {
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(1, 23, 59, 59)))
    }

    @Test
    fun `overnight window is active just after midnight on the following calendar day`() {
        // Rule only selects Monday; 00:30 on Tuesday (Jan 2) is still "the Monday session"
        // continuing past midnight.
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(2, 0, 30)))
    }

    @Test
    fun `overnight window is active at 00-00-00 exactly (midnight boundary itself)`() {
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(2, 0, 0, 0)))
    }

    @Test
    fun `overnight window is active one second before end`() {
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(2, 0, 59, 59)))
    }

    @Test
    fun `overnight window is inactive at exact end time (end is exclusive)`() {
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(2, 1, 0, 0)))
    }

    @Test
    fun `overnight window active-day check uses the day the window STARTED on, not the day it ends on`() {
        // Rule selects only Monday. Tuesday night (Jan 2, 23:00) must NOT start a session,
        // because Tuesday is not selected — even though the window would spill into
        // Wednesday, which is irrelevant here.
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(2, 23, 0, 0)))
    }

    @Test
    fun `overnight window does not leak into the next night if only the start day is selected`() {
        // Confirms 00:30 on Wednesday (Jan 3) is NOT covered by a Monday-only overnight rule
        // (that would only be true if Tuesday were also selected).
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY))
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(3, 0, 30)))
    }

    // ---- Every day repeat -----------------------------------------------------------

    @Test
    fun `every-day rule is active on all seven days at the same time`() {
        val rule = sameDayRule(days = everyDay)
        for (day in 1..7) {
            assertTrue("day $day should be active", ScheduleEngine.isRuleActive(rule, dt(day, 8, 0)))
        }
    }

    // ---- Selected weekdays only -------------------------------------------------------

    @Test
    fun `rule restricted to specific weekdays is inactive on unselected days`() {
        // Jan 1 = Monday, Jan 3 = Wednesday, Jan 5 = Friday selected; Jan 2 = Tuesday is not.
        val rule = sameDayRule(days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(1, 8, 0)))
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(2, 8, 0)))
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(3, 8, 0)))
        assertTrue(ScheduleEngine.isRuleActive(rule, dt(5, 8, 0)))
    }

    // ---- Disabled rules -----------------------------------------------------------------

    @Test
    fun `disabled rule is never active even squarely inside its time window`() {
        val rule = sameDayRule(days = everyDay).copy(isEnabled = false)
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(1, 8, 0)))
    }

    @Test
    fun `disabled overnight rule is never active even across the midnight boundary`() {
        val rule = overnightRule(days = everyDay).copy(isEnabled = false)
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(2, 0, 30)))
    }

    // ---- Degenerate rule (start == end) --------------------------------------------------

    @Test
    fun `rule with equal start and end minute is never active and does not throw`() {
        val rule = FocusRule(
            id = 1,
            name = "Degenerate",
            startMinuteOfDay = 480,
            endMinuteOfDay = 480,
            repeatDays = everyDay
        )
        assertEquals(0, rule.durationMinutes)
        assertFalse(ScheduleEngine.isRuleActive(rule, dt(1, 8, 0)))
        assertEquals(Duration.ZERO, ScheduleEngine.getRemainingDuration(rule, dt(1, 8, 0)))
    }

    // ---- Multiple / overlapping rules -----------------------------------------------------

    @Test
    fun `getActiveRules returns only the rules active at the given moment`() {
        val morning = sameDayRule(days = everyDay, name = "Morning")
        val overnight = overnightRule(days = setOf(DayOfWeek.MONDAY), name = "Overnight")
        val disabled = sameDayRule(days = everyDay, name = "Disabled").copy(isEnabled = false)

        val rules = listOf(morning, overnight, disabled)

        val activeAt0830 = ScheduleEngine.getActiveRules(rules, dt(1, 8, 30))
        assertEquals(listOf(morning), activeAt0830)

        val activeAtMidnightThirty = ScheduleEngine.getActiveRules(rules, dt(2, 0, 30))
        assertEquals(listOf(overnight), activeAtMidnightThirty)
    }

    @Test
    fun `overlapping rules are both returned as active`() {
        val ruleA = sameDayRule(days = everyDay, name = "A", start = 7 * 60, end = 10 * 60)
        val ruleB = sameDayRule(days = everyDay, name = "B", start = 9 * 60, end = 11 * 60)

        val activeAt930 = ScheduleEngine.getActiveRules(listOf(ruleA, ruleB), dt(1, 9, 30))
        assertEquals(2, activeAt930.size)
        assertTrue(activeAt930.containsAll(listOf(ruleA, ruleB)))
    }

    // ---- getRemainingDuration ---------------------------------------------------------------

    @Test
    fun `remaining duration for an inactive rule is zero`() {
        val rule = sameDayRule()
        assertEquals(Duration.ZERO, ScheduleEngine.getRemainingDuration(rule, dt(1, 6, 0)))
    }

    @Test
    fun `remaining duration at exact start of a same-day window equals the full window`() {
        val rule = sameDayRule() // 07:00 -> 10:00
        val remaining = ScheduleEngine.getRemainingDuration(rule, dt(1, 7, 0, 0))
        assertEquals(Duration.ofHours(3), remaining)
    }

    @Test
    fun `remaining duration one second before end is one second`() {
        val rule = sameDayRule() // 07:00 -> 10:00
        val remaining = ScheduleEngine.getRemainingDuration(rule, dt(1, 9, 59, 59))
        assertEquals(Duration.ofSeconds(1), remaining)
    }

    @Test
    fun `remaining duration for overnight window measured before midnight`() {
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY)) // 23:00 -> 01:00
        val remaining = ScheduleEngine.getRemainingDuration(rule, dt(1, 23, 0, 0))
        assertEquals(Duration.ofHours(2), remaining)
    }

    @Test
    fun `remaining duration for overnight window measured after midnight`() {
        val rule = overnightRule(days = setOf(DayOfWeek.MONDAY)) // 23:00 -> 01:00
        val remaining = ScheduleEngine.getRemainingDuration(rule, dt(2, 0, 30, 0))
        assertEquals(Duration.ofMinutes(30), remaining)
    }

    // ---- Local-time contract (documentation-style check, not a zone-conversion test) ------

    @Test
    fun `engine compares wall-clock time only and is agnostic to which zone produced it`() {
        // ScheduleEngine takes a LocalDateTime, so this test simply demonstrates that two
        // LocalDateTimes built via different ZoneIds but landing on the same wall-clock
        // instant-of-day evaluate identically — the engine itself never sees a ZoneId. This
        // documents the contract described in ScheduleEngine's KDoc: callers are responsible
        // for resolving to local time (e.g. LocalDateTime.now(ZoneId.systemDefault())) before
        // calling in.
        val rule = sameDayRule()
        val viaKabulWallClock = LocalDateTime.of(2024, 1, 1, 8, 0)
        val viaUtcWallClock = LocalDateTime.of(2024, 1, 1, 8, 0)
        assertEquals(
            ScheduleEngine.isRuleActive(rule, viaKabulWallClock),
            ScheduleEngine.isRuleActive(rule, viaUtcWallClock)
        )
    }

    // ---- Fixtures -----------------------------------------------------------------------------

    private fun sameDayRule(
        name: String = "Morning Focus",
        days: Set<DayOfWeek> = everyDay,
        start: Int = 7 * 60,
        end: Int = 10 * 60
    ): FocusRule = FocusRule(
        id = 1,
        name = name,
        startMinuteOfDay = start,
        endMinuteOfDay = end,
        repeatDays = days
    )

    private fun overnightRule(
        name: String = "Night Focus",
        days: Set<DayOfWeek>,
        start: Int = 23 * 60,
        end: Int = 60
    ): FocusRule = FocusRule(
        id = 2,
        name = name,
        startMinuteOfDay = start,
        endMinuteOfDay = end,
        repeatDays = days
    )
}
