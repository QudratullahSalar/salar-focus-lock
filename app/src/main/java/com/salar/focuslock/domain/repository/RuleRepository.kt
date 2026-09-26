package com.salar.focuslock.domain.repository

import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing access to FocusRules and their BlockedApps. No Room/Android types leak
 * through this interface — implementations (data/repository/RuleRepositoryImpl.kt) own the
 * entity<->domain mapping.
 */
interface RuleRepository {
    fun observeRules(): Flow<List<FocusRule>>
    suspend fun getRuleById(id: Long): FocusRule?
    suspend fun getEnabledRules(): List<FocusRule>

    /** Inserts if [rule].id == 0L, otherwise replaces the existing row. Returns the row id. */
    suspend fun upsertRule(rule: FocusRule): Long
    suspend fun deleteRule(rule: FocusRule)

    fun observeBlockedApps(ruleId: Long): Flow<List<BlockedApp>>

    /**
     * One-shot (non-Flow) lookup of the blocked apps for [ruleId], for decision-time callers
     * like BlockingEngine that need a single synchronous snapshot rather than an ongoing
     * subscription.
     */
    suspend fun getBlockedApps(ruleId: Long): List<BlockedApp>

    /** Replaces the full blocked-app set for [ruleId] with [apps] in one operation. */
    suspend fun setBlockedApps(ruleId: Long, apps: List<BlockedApp>)
}
