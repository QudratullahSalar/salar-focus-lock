package com.salar.focuslock.domain.model

import java.time.Instant

/**
 * A record of one continuous period during which a [FocusRule] was actively enforced.
 * [ruleId] is nullable because the originating rule may be deleted later while the historical
 * session record is kept (see the Room ForeignKey onDelete = SET_NULL in FocusSessionEntity).
 * [endedAt] is null while the session is still in progress; [completed] distinguishes a
 * session that ran naturally to the rule's end time from one cut short (app killed, rule
 * disabled mid-way, permissions revoked, etc.).
 */
data class FocusSession(
    val id: Long = 0L,
    val ruleId: Long?,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val completed: Boolean = false,
    val blockedAttemptCount: Int = 0
)
