package com.salar.focuslock.domain.scheduler

import com.salar.focuslock.domain.model.FocusRule
import java.time.Duration
import java.time.LocalDateTime

/**
 * Pure, stateless time-window logic for [FocusRule]s. No Android dependency, no I/O —
 * every answer is recomputed from the rule's stored fields and the [currentDateTime] passed
 * in, so nothing here trusts an in-memory timer or survives-a-restart assumption; the caller
 * (WorkManager job / BootReceiver / AccessibilityService, wired in a later batch) is
 * responsible for re-invoking this on every relevant event.
 *
 * Timezone handling: all functions take a [LocalDateTime], which has no attached zone. The
 * caller must supply it already resolved to the device's local wall-clock time, e.g.
 * `LocalDateTime.now(ZoneId.systemDefault())`. This is intentional — a focus rule set for
 * "07:00" means 7am wherever the phone currently is, not a fixed instant, so the engine only
 * ever compares local wall-clock minutes and weekdays and never touches [java.time.Instant]
 * or UTC. This also means the engine behaves correctly across a manual device time change or
 * a travel-induced zone change without any special-casing: whatever LocalDateTime the caller
 * supplies is simply taken at face value.
 */
object ScheduleEngine {

    /**
     * Whether [rule] is active at [currentDateTime].
     *
     * - A disabled rule ([FocusRule.isEnabled] == false) is never active.
     * - A degenerate rule (startMinuteOfDay == endMinuteOfDay) is never active.
     * - The start boundary is inclusive, the end boundary is exclusive: a rule for
     *   07:00 -> 10:00 is active starting exactly at 07:00:00 and is no longer active
     *   starting exactly at 10:00:00.
     * - Overnight rules (endMinuteOfDay < startMinuteOfDay, e.g. 23:00 -> 01:00) are
     *   evaluated as belonging to the day they *start* on: [FocusRule.repeatDays] is checked
     *   against the day the 23:00 boundary falls on, not the day the 01:00 boundary falls on.
     */
    fun isRuleActive(rule: FocusRule, currentDateTime: LocalDateTime): Boolean {
        if (!rule.isEnabled) return false
        if (rule.durationMinutes == 0) return false

        val currentMinute = currentDateTime.minuteOfDay()
        val currentDay = currentDateTime.dayOfWeek

        return if (!rule.isOvernight) {
            rule.repeatDays.contains(currentDay) &&
                currentMinute >= rule.startMinuteOfDay &&
                currentMinute < rule.endMinuteOfDay
        } else {
            val startedToday = rule.repeatDays.contains(currentDay) &&
                currentMinute >= rule.startMinuteOfDay
            val continuingFromYesterday = rule.repeatDays.contains(currentDay.minus(1)) &&
                currentMinute < rule.endMinuteOfDay
            startedToday || continuingFromYesterday
        }
    }

    /**
     * All rules from [rules] that are active at [currentDateTime]. Overlapping rules (two or
     * more rules simultaneously active, whether or not they share blocked apps) are all
     * returned — this function does no merging or de-duplication of blocked apps; that's the
     * responsibility of whatever consumes this list (a use case in a later batch).
     */
    fun getActiveRules(rules: List<FocusRule>, currentDateTime: LocalDateTime): List<FocusRule> =
        rules.filter { isRuleActive(it, currentDateTime) }

    /**
     * Time remaining until [rule]'s current active window ends, evaluated at [currentDateTime].
     * Returns [Duration.ZERO] if [rule] is not active at [currentDateTime] (including disabled
     * or degenerate rules) — this function does not throw for an inactive rule, it simply
     * reports no remaining time.
     */
    fun getRemainingDuration(rule: FocusRule, currentDateTime: LocalDateTime): Duration {
        if (!isRuleActive(rule, currentDateTime)) return Duration.ZERO

        val currentMinute = currentDateTime.minuteOfDay()
        val currentSecond = currentDateTime.second

        val remainingWholeMinutes: Int = if (!rule.isOvernight) {
            rule.endMinuteOfDay - currentMinute
        } else {
            val currentDay = currentDateTime.dayOfWeek
            val startedToday = rule.repeatDays.contains(currentDay) &&
                currentMinute >= rule.startMinuteOfDay
            if (startedToday) {
                (FocusRule.MINUTES_PER_DAY - currentMinute) + rule.endMinuteOfDay
            } else {
                rule.endMinuteOfDay - currentMinute
            }
        }

        val remainingSeconds = (remainingWholeMinutes.toLong() * 60L) - currentSecond
        return Duration.ofSeconds(remainingSeconds.coerceAtLeast(0L))
    }

    private fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute
}
