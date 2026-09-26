package com.salar.focuslock.domain.blocking

import com.salar.focuslock.domain.model.FocusLockScreenState
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.scheduler.ScheduleEngine
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure recomputation of what the Focus Lock screen should show, given the persisted FocusRule
 * and the current time. Per requirement #5, the screen must never trust a countdown timer as
 * its source of truth — this is the only way FocusLockScreenState is ever produced, and it is
 * meant to be re-invoked with a fresh LocalDateTime on every UI tick and again from scratch on
 * any Activity recreation/configuration change, always reading the rule fresh from
 * RuleRepository rather than carrying state forward.
 *
 * Deliberately looks up exactly one rule (by the id BlockingEngine already chose in
 * BlockDecision) rather than re-scanning all rules — BlockingEngine's deterministic
 * "tightest remaining time wins" selection (see its KDoc) is reused as-is, not duplicated here.
 */
object FocusLockScreenStateCalculator {

    /**
     * Returns null when there is nothing valid to show: [rule] is null, e.g. because it was
     * deleted or its id no longer resolves since the redirect was launched. Callers should
     * treat a null result the same as [FocusLockScreenState.isStillActive] == false — stop
     * showing the lock screen and let the user proceed.
     */
    fun calculate(
        rule: FocusRule?,
        packageName: String,
        appName: String,
        currentDateTime: LocalDateTime
    ): FocusLockScreenState? {
        if (rule == null) return null

        val isActive = ScheduleEngine.isRuleActive(rule, currentDateTime)
        val remaining = ScheduleEngine.getRemainingDuration(rule, currentDateTime)
        val secondsPerDay = FocusRule.MINUTES_PER_DAY * 60L
        val unlockAt = LocalTime.ofSecondOfDay((rule.endMinuteOfDay.toLong() * 60L) % secondsPerDay)

        return FocusLockScreenState(
            packageName = packageName,
            appName = appName,
            ruleName = rule.name,
            remainingDuration = remaining,
            unlockAt = unlockAt,
            isStillActive = isActive
        )
    }
}
