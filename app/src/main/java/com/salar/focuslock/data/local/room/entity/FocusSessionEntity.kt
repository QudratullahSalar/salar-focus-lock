package com.salar.focuslock.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a FocusSession. Unlike BlockedApp, this uses onDelete = SET_NULL rather than
 * CASCADE: deleting a FocusRule should not erase the historical record that a focus session
 * happened under it — the session just becomes "rule since deleted" (ruleId = null) so
 * lifetime statistics stay accurate.
 */
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = FocusRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("ruleId")]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ruleId: Long?,
    val startedAt: Long,
    val endedAt: Long?,
    val completed: Boolean,
    val blockedAttemptCount: Int
)
