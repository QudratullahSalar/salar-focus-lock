package com.salar.focuslock.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for a FocusRule. All fields are primitive-safe (Long/Int/String/Boolean) so no
 * TypeConverter is needed:
 * - [repeatDaysMask] stores the Set<DayOfWeek> as a 7-bit mask (bit 0 = Monday .. bit 6 = Sunday,
 *   matching java.time.DayOfWeek.value 1..7) — see data/mapper/RuleMappers.kt.
 * - [createdAt] / [updatedAt] store Instant as epoch milliseconds.
 */
@Entity(tableName = "focus_rules")
data class FocusRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val repeatDaysMask: Int,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
