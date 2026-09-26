package com.salar.focuslock.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a BlockedApp. Cascade-deletes when its parent FocusRule is deleted — a rule
 * with no blocked apps left behind doesn't make sense to keep.
 */
@Entity(
    tableName = "blocked_apps",
    foreignKeys = [
        ForeignKey(
            entity = FocusRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ruleId")]
)
data class BlockedAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ruleId: Long,
    val packageName: String,
    val appLabelCache: String,
    val iconUriCache: String?
)
