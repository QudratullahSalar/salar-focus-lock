package com.salar.focuslock.domain.rules

import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/** Written but NOT executed locally (no JVM/Gradle in the authoring environment). */
class RuleDraftTest {

    private fun valid() = RuleDraft(
        name = "Morning Focus",
        startMinuteOfDay = 7 * 60,
        endMinuteOfDay = 10 * 60,
        repeatDays = DayOfWeek.values().toSet(),
        selectedApps = mapOf("com.example.tiktok" to "TikTok")
    )

    @Test
    fun `valid draft has no error`() {
        assertNull(valid().validate())
    }

    @Test
    fun `blank or whitespace name is rejected`() {
        assertEquals(RuleDraftError.NAME_EMPTY, valid().copy(name = "").validate())
        assertEquals(RuleDraftError.NAME_EMPTY, valid().copy(name = "   ").validate())
    }

    @Test
    fun `name longer than the limit is rejected, exactly at the limit is accepted`() {
        assertEquals(
            RuleDraftError.NAME_TOO_LONG,
            valid().copy(name = "a".repeat(RuleDraft.MAX_NAME_LENGTH + 1)).validate()
        )
        assertNull(valid().copy(name = "a".repeat(RuleDraft.MAX_NAME_LENGTH)).validate())
    }

    @Test
    fun `start equal to end is rejected`() {
        assertEquals(
            RuleDraftError.START_EQUALS_END,
            valid().copy(startMinuteOfDay = 480, endMinuteOfDay = 480).validate()
        )
    }

    @Test
    fun `out of range minutes are rejected`() {
        assertEquals(RuleDraftError.INVALID_TIME, valid().copy(startMinuteOfDay = -1).validate())
        assertEquals(RuleDraftError.INVALID_TIME, valid().copy(endMinuteOfDay = 1440).validate())
    }

    @Test
    fun `no days is rejected`() {
        assertEquals(RuleDraftError.NO_DAYS, valid().copy(repeatDays = emptySet()).validate())
    }

    @Test
    fun `no apps is rejected`() {
        assertEquals(RuleDraftError.NO_APPS, valid().copy(selectedApps = emptyMap()).validate())
    }

    @Test
    fun `overnight draft is valid and flagged as overnight`() {
        val overnight = valid().copy(startMinuteOfDay = 23 * 60, endMinuteOfDay = 60)
        assertNull(overnight.validate())
        assertTrue(overnight.isOvernight)
        assertFalse(valid().isOvernight)
    }

    @Test
    fun `from maps a saved rule and its blocked apps into a draft`() {
        val rule = FocusRule(
            id = 5,
            name = "Night",
            startMinuteOfDay = 23 * 60,
            endMinuteOfDay = 60,
            repeatDays = setOf(DayOfWeek.MONDAY)
        )
        val apps = listOf(
            BlockedApp(id = 1, ruleId = 5, packageName = "com.a", appLabelCache = "A"),
            BlockedApp(id = 2, ruleId = 5, packageName = "com.b", appLabelCache = "B")
        )

        val draft = RuleDraft.from(rule, apps)

        assertEquals("Night", draft.name)
        assertEquals(23 * 60, draft.startMinuteOfDay)
        assertEquals(60, draft.endMinuteOfDay)
        assertEquals(setOf(DayOfWeek.MONDAY), draft.repeatDays)
        assertEquals(mapOf("com.a" to "A", "com.b" to "B"), draft.selectedApps)
    }
}
