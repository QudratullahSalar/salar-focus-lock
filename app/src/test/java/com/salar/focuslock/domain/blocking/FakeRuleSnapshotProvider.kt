package com.salar.focuslock.domain.blocking

import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.repository.RuleSnapshotProvider

/** In-memory test double for RuleSnapshotProvider — exactly what BlockingEngine now depends on. */
class FakeRuleSnapshotProvider(
    private val rules: List<FocusRule> = emptyList(),
    private val blockedAppsByRuleId: Map<Long, List<BlockedApp>> = emptyMap()
) : RuleSnapshotProvider {
    override fun getEnabledRulesSnapshot(): List<FocusRule> = rules.filter { it.isEnabled }
    override fun getBlockedAppsSnapshot(ruleId: Long): List<BlockedApp> = blockedAppsByRuleId[ruleId].orEmpty()
}
