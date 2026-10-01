package com.salar.focuslock.domain.blocking

import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

/**
 * Unit tests for BlockingEngine, using FakeRuleSnapshotProvider so no Room/Android/Hilt/
 * coroutines is involved — BlockingEngine.evaluate() is now plain synchronous code (see its
 * KDoc: it reads an in-memory RuleSnapshotProvider, not RuleRepository, specifically so the
 * real AccessibilityService path has no suspend/Room round trip on it).
 *
 * IMPORTANT: these tests were written but have NOT been executed — no JVM/Gradle is available
 * in this environment. They were checked by static/manual read-through against
 * BlockingEngine's and ScheduleEngine's logic only (ScheduleEngine's own behavior was already
 * verified this same way in Batch 1). Treat pass/fail as unverified until run for real with
 * `./gradlew testDebugUnitTest`.
 *
 * Fixture week: Mon 2024-01-01 (matches ScheduleEngineTest's reference week).
 */
class BlockingEngineTest {

    private val everyDay: Set<DayOfWeek> = DayOfWeek.values().toSet()

    private fun dt(day: Int, hour: Int, minute: Int): LocalDateTime =
        LocalDateTime.of(2024, 1, day, hour, minute)

    private fun rule(
        id: Long,
        name: String = "Rule $id",
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

    private fun blockedApp(ruleId: Long, packageName: String): BlockedApp =
        BlockedApp(id = 0L, ruleId = ruleId, packageName = packageName, appLabelCache = packageName)

    @Test
    fun `no rules at all yields shouldBlock false`() {
        val engine = BlockingEngine(FakeRuleSnapshotProvider())
        val decision = engine.evaluate("com.example.tiktok", dt(1, 8, 0))

        assertFalse(decision.shouldBlock)
        assertEquals("com.example.tiktok", decision.packageName)
        assertNull(decision.ruleId)
        assertNull(decision.ruleName)
        assertEquals(Duration.ZERO, decision.remainingDuration)
    }

    @Test
    fun `active rule with matching package yields shouldBlock true with correct rule info`() {
        val r = rule(id = 1, name = "Morning Focus")
        val provider = FakeRuleSnapshotProvider(
            rules = listOf(r),
            blockedAppsByRuleId = mapOf(1L to listOf(blockedApp(1, "com.example.tiktok")))
        )
        val engine = BlockingEngine(provider)

        val decision = engine.evaluate("com.example.tiktok", dt(1, 8, 0))

        assertTrue(decision.shouldBlock)
        assertEquals(1L, decision.ruleId)
        assertEquals("Morning Focus", decision.ruleName)
        assertEquals(Duration.ofHours(2), decision.remainingDuration) // 08:00 -> 10:00
    }

    @Test
    fun `active rule with non-matching package yields shouldBlock false`() {
        val r = rule(id = 1)
        val provider = FakeRuleSnapshotProvider(
            rules = listOf(r),
            blockedAppsByRuleId = mapOf(1L to listOf(blockedApp(1, "com.example.tiktok")))
        )
        val engine = BlockingEngine(provider)

        val decision = engine.evaluate("com.example.maps", dt(1, 8, 0))

        assertFalse(decision.shouldBlock)
        assertNull(decision.ruleId)
    }

    @Test
    fun `multiple blocked apps under one rule all resolve to shouldBlock true`() {
        val r = rule(id = 1)
        val provider = FakeRuleSnapshotProvider(
            rules = listOf(r),
            blockedAppsByRuleId = mapOf(
                1L to listOf(
                    blockedApp(1, "com.example.tiktok"),
                    blockedApp(1, "com.example.instagram"),
                    blockedApp(1, "com.example.youtube")
                )
            )
        )
        val engine = BlockingEngine(provider)

        assertTrue(engine.evaluate("com.example.tiktok", dt(1, 8, 0)).shouldBlock)
        assertTrue(engine.evaluate("com.example.instagram", dt(1, 8, 0)).shouldBlock)
        assertTrue(engine.evaluate("com.example.youtube", dt(1, 8, 0)).shouldBlock)
        assertFalse(engine.evaluate("com.example.maps", dt(1, 8, 0)).shouldBlock)
    }

    @Test
    fun `multiple active rules covering different packages each resolve independently`() {
        val morning = rule(id = 1, name = "Morning", start = 7 * 60, end = 10 * 60)
        val overnight = rule(
            id = 2,
            name = "Night",
            days = setOf(DayOfWeek.MONDAY),
            start = 23 * 60,
            end = 60
        )
        val provider = FakeRuleSnapshotProvider(
            rules = listOf(morning, overnight),
            blockedAppsByRuleId = mapOf(
                1L to listOf(blockedApp(1, "com.example.tiktok")),
                2L to listOf(blockedApp(2, "com.example.game"))
            )
        )
        val engine = BlockingEngine(provider)

        val tiktokDecision = engine.evaluate("com.example.tiktok", dt(1, 8, 30))
        assertTrue(tiktokDecision.shouldBlock)
        assertEquals(1L, tiktokDecision.ruleId)

        val gameAt0830 = engine.evaluate("com.example.game", dt(1, 8, 30))
        assertFalse(gameAt0830.shouldBlock) // overnight rule not active yet

        val gameAtMidnightThirty = engine.evaluate("com.example.game", dt(2, 0, 30))
        assertTrue(gameAtMidnightThirty.shouldBlock)
        assertEquals(2L, gameAtMidnightThirty.ruleId)
    }

    @Test
    fun `disabled rule never blocks even with a matching package inside its time window`() {
        val r = rule(id = 1, enabled = false)
        val provider = FakeRuleSnapshotProvider(
            rules = listOf(r),
            blockedAppsByRuleId = mapOf(1L to listOf(blockedApp(1, "com.example.tiktok")))
        )
        val engine = BlockingEngine(provider)

        val decision = engine.evaluate("com.example.tiktok", dt(1, 8, 0))
        assertFalse(decision.shouldBlock)
    }

    @Test
    fun `rule outside its schedule window does not block`() {
        val r = rule(id = 1, start = 7 * 60, end = 10 * 60)
        val provider = FakeRuleSnapshotProvider(
            rules = listOf(r),
            blockedAppsByRuleId = mapOf(1L to listOf(blockedApp(1, "com.example.tiktok")))
        )
        val engine = BlockingEngine(provider)

        val decision = engine.evaluate("com.example.tiktok", dt(1, 12, 0)) // noon, well after 10:00
        assertFalse(decision.shouldBlock)
    }

    @Test
    fun `remaining duration is passed through correctly from ScheduleEngine`() {
        val r = rule(id = 1, start = 7 * 60, end = 10 * 60)
        val provider = FakeRuleSnapshotProvider(
            rules = listOf(r),
            blockedAppsByRuleId = mapOf(1L to listOf(blockedApp(1, "com.example.tiktok")))
        )
        val engine = BlockingEngine(provider)

        val decision = engine.evaluate("com.example.tiktok", dt(1, 9, 59)) // one minute before end
        assertTrue(decision.shouldBlock)
        assertEquals(Duration.ofMinutes(1), decision.remainingDuration)
    }

    @Test
    fun `two active rules blocking the same package resolve to the one with least remaining time`() {
        val ruleA = rule(id = 1, name = "Wide", start = 7 * 60, end = 10 * 60)
        val ruleB = rule(id = 2, name = "Narrow", start = 9 * 60, end = 9 * 60 + 30)
        val provider = FakeRuleSnapshotProvider(
            rules = listOf(ruleA, ruleB),
            blockedAppsByRuleId = mapOf(
                1L to listOf(blockedApp(1, "com.example.tiktok")),
                2L to listOf(blockedApp(2, "com.example.tiktok"))
            )
        )
        val engine = BlockingEngine(provider)

        val decision = engine.evaluate("com.example.tiktok", dt(1, 9, 15))

        assertTrue(decision.shouldBlock)
        assertEquals(2L, decision.ruleId)
        assertEquals("Narrow", decision.ruleName)
        assertEquals(Duration.ofMinutes(15), decision.remainingDuration)
    }
}
