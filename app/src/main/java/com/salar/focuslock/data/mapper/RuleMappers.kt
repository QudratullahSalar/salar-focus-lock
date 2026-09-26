package com.salar.focuslock.data.mapper

import com.salar.focuslock.data.local.room.entity.BlockedAppEntity
import com.salar.focuslock.data.local.room.entity.FocusRuleEntity
import com.salar.focuslock.data.local.room.entity.FocusSessionEntity
import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.model.FocusSession
import java.time.DayOfWeek
import java.time.Instant

// --- DayOfWeek <-> bitmask -------------------------------------------------
// bit (day.value - 1) is set for each selected day: bit 0 = Monday .. bit 6 = Sunday,
// matching java.time.DayOfWeek.value (Monday = 1 .. Sunday = 7).

fun Set<DayOfWeek>.toBitmask(): Int =
    fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }

fun Int.toDayOfWeekSet(): Set<DayOfWeek> =
    // DayOfWeek is a java.time (Java-declared) enum, so Kotlin's synthetic `entries` property
    // (which only exists for Kotlin-declared enums) is not available here — use values().
    DayOfWeek.values().filterTo(mutableSetOf()) { day -> (this shr (day.value - 1)) and 1 == 1 }

// --- FocusRule ---------------------------------------------------------------

fun FocusRuleEntity.toDomain(): FocusRule = FocusRule(
    id = id,
    name = name,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    repeatDays = repeatDaysMask.toDayOfWeekSet(),
    isEnabled = isEnabled,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

fun FocusRule.toEntity(): FocusRuleEntity = FocusRuleEntity(
    id = id,
    name = name,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    repeatDaysMask = repeatDays.toBitmask(),
    isEnabled = isEnabled,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

// --- BlockedApp ----------------------------------------------------------------

fun BlockedAppEntity.toDomain(): BlockedApp = BlockedApp(
    id = id,
    ruleId = ruleId,
    packageName = packageName,
    appLabelCache = appLabelCache,
    iconUriCache = iconUriCache
)

fun BlockedApp.toEntity(): BlockedAppEntity = BlockedAppEntity(
    id = id,
    ruleId = ruleId,
    packageName = packageName,
    appLabelCache = appLabelCache,
    iconUriCache = iconUriCache
)

// --- FocusSession ----------------------------------------------------------------

fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    ruleId = ruleId,
    startedAt = Instant.ofEpochMilli(startedAt),
    endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
    completed = completed,
    blockedAttemptCount = blockedAttemptCount
)

fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    ruleId = ruleId,
    startedAt = startedAt.toEpochMilli(),
    endedAt = endedAt?.toEpochMilli(),
    completed = completed,
    blockedAttemptCount = blockedAttemptCount
)
