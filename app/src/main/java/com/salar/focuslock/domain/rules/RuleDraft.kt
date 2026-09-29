package com.salar.focuslock.domain.rules

import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import java.time.DayOfWeek

enum class RuleDraftError {
    NAME_EMPTY,
    NAME_TOO_LONG,
    INVALID_TIME,
    START_EQUALS_END,
    NO_DAYS,
    NO_APPS
}

/**
 * The in-progress, editable form of a focus rule (what the rule editor screen holds before
 * Save). Pure Kotlin so the validation rules are unit-testable.
 *
 * [selectedApps] maps package name -> display label (the label is cached into BlockedApp).
 */
data class RuleDraft(
    val name: String = "",
    val startMinuteOfDay: Int = 7 * 60,
    val endMinuteOfDay: Int = 10 * 60,
    val repeatDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
    val selectedApps: Map<String, String> = emptyMap()
) {
    val isOvernight: Boolean get() = endMinuteOfDay < startMinuteOfDay

    /** Null when the draft is valid and can be saved. */
    fun validate(): RuleDraftError? {
        val validMinutes = 0..(FocusRule.MINUTES_PER_DAY - 1)
        return when {
            name.isBlank() -> RuleDraftError.NAME_EMPTY
            name.trim().length > MAX_NAME_LENGTH -> RuleDraftError.NAME_TOO_LONG
            startMinuteOfDay !in validMinutes || endMinuteOfDay !in validMinutes -> RuleDraftError.INVALID_TIME
            startMinuteOfDay == endMinuteOfDay -> RuleDraftError.START_EQUALS_END
            repeatDays.isEmpty() -> RuleDraftError.NO_DAYS
            selectedApps.isEmpty() -> RuleDraftError.NO_APPS
            else -> null
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40

        fun from(rule: FocusRule, apps: List<BlockedApp>): RuleDraft = RuleDraft(
            name = rule.name,
            startMinuteOfDay = rule.startMinuteOfDay,
            endMinuteOfDay = rule.endMinuteOfDay,
            repeatDays = rule.repeatDays,
            selectedApps = apps.associate { it.packageName to it.appLabelCache }
        )
    }
}
