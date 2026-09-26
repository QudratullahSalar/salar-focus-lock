package com.salar.focuslock.domain.model

import java.time.Duration

/**
 * The outcome of BlockingEngine.evaluate() for one foreground-package observation.
 *
 * When [shouldBlock] is false, [ruleId]/[ruleName] are null and [remainingDuration] is
 * [Duration.ZERO] — there is no "the rule" to report because no active rule blocks this
 * package. When [shouldBlock] is true, all three describe the single rule BlockingEngine
 * selected (see its tie-breaking rule when more than one active rule blocks the same package).
 */
data class BlockDecision(
    val shouldBlock: Boolean,
    val packageName: String,
    val ruleId: Long? = null,
    val ruleName: String? = null,
    val remainingDuration: Duration = Duration.ZERO
)
