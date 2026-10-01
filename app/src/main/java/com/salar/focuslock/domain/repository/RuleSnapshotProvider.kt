package com.salar.focuslock.domain.repository

import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule

/**
 * A synchronous, already-in-memory view of enabled rules and their blocked apps — what
 * BlockingEngine needs to evaluate a single foreground-package event as fast as possible,
 * without waiting on a Room query on that hot path (see RuleCache, the implementation, for why
 * this exists — it's the fix for the detection-latency flicker reported after Batch 4).
 *
 * Deliberately separate from RuleRepository (which stays suspend/Flow-based and remains the
 * actual source of truth for everything else — the rule list UI, the editor, the lock screen's
 * own per-second refresh) rather than adding sync methods to it, the same way
 * AppIconProvider was split out from AppDiscoveryRepository: a different access pattern for a
 * different kind of caller.
 */
interface RuleSnapshotProvider {
    fun getEnabledRulesSnapshot(): List<FocusRule>
    fun getBlockedAppsSnapshot(ruleId: Long): List<BlockedApp>
}
