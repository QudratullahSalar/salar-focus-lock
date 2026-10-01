package com.salar.focuslock.domain.blocking

import com.salar.focuslock.domain.model.BlockDecision
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.repository.RuleSnapshotProvider
import com.salar.focuslock.domain.scheduler.ScheduleEngine
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Sits between the AccessibilityService (or any future caller) and the schedule/data layers:
 *
 *   AccessibilityService -> BlockingEngine -> ScheduleEngine -> RuleSnapshotProvider -> (RuleCache mirrors Room)
 *
 * BlockingEngine owns no business logic of its own beyond "which active rule, if any, blocks
 * this package" — the actual time-window math is ScheduleEngine's (unit-tested in Batch 1).
 *
 * Reads from RuleSnapshotProvider (an in-memory mirror, see RuleCache) rather than from
 * RuleRepository directly: this method is called once per AccessibilityService foreground-
 * change event, so it is deliberately plain/synchronous — no suspend, no Room round trip — to
 * minimize the detection-to-redirect latency window. That latency was a real reported bug
 * (the blocked app briefly flickering visible/interactive before the lock screen took over).
 */
class BlockingEngine @Inject constructor(
    private val ruleSnapshotProvider: RuleSnapshotProvider
) {

    /**
     * Decides whether [packageName] should currently be blocked, evaluated at [currentDateTime].
     *
     * If more than one currently-active rule blocks the same package (overlapping rules that
     * share a blocked app), the rule with the LEAST remaining time is reported — the tightest,
     * soonest-to-end constraint is the most actionable one to show the user. This is a
     * deliberate, deterministic tie-break, not an arbitrary "first match wins".
     */
    fun evaluate(packageName: String, currentDateTime: LocalDateTime): BlockDecision {
        val enabledRules = ruleSnapshotProvider.getEnabledRulesSnapshot()
        val activeRules = ScheduleEngine.getActiveRules(enabledRules, currentDateTime)

        var winningRule: FocusRule? = null
        var winningRemaining: Duration? = null

        for (rule in activeRules) {
            val blockedApps = ruleSnapshotProvider.getBlockedAppsSnapshot(rule.id)
            val blocksThisPackage = blockedApps.any { it.packageName == packageName }
            if (!blocksThisPackage) continue

            val remaining = ScheduleEngine.getRemainingDuration(rule, currentDateTime)
            if (winningRemaining == null || remaining < winningRemaining) {
                winningRule = rule
                winningRemaining = remaining
            }
        }

        val rule = winningRule
        val remaining = winningRemaining
        return if (rule != null && remaining != null) {
            BlockDecision(
                shouldBlock = true,
                packageName = packageName,
                ruleId = rule.id,
                ruleName = rule.name,
                remainingDuration = remaining
            )
        } else {
            BlockDecision(shouldBlock = false, packageName = packageName)
        }
    }
}
