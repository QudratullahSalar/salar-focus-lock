package com.salar.focuslock.domain.blocking

import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory RuleRepository test double. Implements the full interface (so it stays a valid
 * substitute regardless of which methods a future test exercises) but only observeRules
 * data is actually exercised by BlockingEngineTest today: getEnabledRules() and
 * getBlockedApps(ruleId) are the two calls BlockingEngine makes.
 */
class FakeRuleRepository(
    private val rules: List<FocusRule> = emptyList(),
    private val blockedAppsByRuleId: Map<Long, List<BlockedApp>> = emptyMap()
) : RuleRepository {

    override fun observeRules(): Flow<List<FocusRule>> = flowOf(rules)

    override suspend fun getRuleById(id: Long): FocusRule? = rules.find { it.id == id }

    override suspend fun getEnabledRules(): List<FocusRule> = rules.filter { it.isEnabled }

    override suspend fun upsertRule(rule: FocusRule): Long = rule.id

    override suspend fun deleteRule(rule: FocusRule) {
        // no-op: not exercised by BlockingEngineTest
    }

    override fun observeBlockedApps(ruleId: Long): Flow<List<BlockedApp>> =
        flowOf(blockedAppsByRuleId[ruleId].orEmpty())

    override suspend fun getBlockedApps(ruleId: Long): List<BlockedApp> =
        blockedAppsByRuleId[ruleId].orEmpty()

    override suspend fun setBlockedApps(ruleId: Long, apps: List<BlockedApp>) {
        // no-op: not exercised by BlockingEngineTest
    }
}
