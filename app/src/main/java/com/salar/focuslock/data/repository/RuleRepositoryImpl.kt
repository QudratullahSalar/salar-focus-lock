package com.salar.focuslock.data.repository

import com.salar.focuslock.data.local.room.dao.BlockedAppDao
import com.salar.focuslock.data.local.room.dao.FocusRuleDao
import com.salar.focuslock.data.mapper.toDomain
import com.salar.focuslock.data.mapper.toEntity
import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RuleRepositoryImpl @Inject constructor(
    private val focusRuleDao: FocusRuleDao,
    private val blockedAppDao: BlockedAppDao
) : RuleRepository {

    override fun observeRules(): Flow<List<FocusRule>> =
        focusRuleDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getRuleById(id: Long): FocusRule? =
        focusRuleDao.getById(id)?.toDomain()

    override suspend fun getEnabledRules(): List<FocusRule> =
        focusRuleDao.getEnabledRules().map { it.toDomain() }

    override suspend fun upsertRule(rule: FocusRule): Long =
        focusRuleDao.insert(rule.toEntity())

    override suspend fun deleteRule(rule: FocusRule) {
        focusRuleDao.delete(rule.toEntity())
    }

    override fun observeBlockedApps(ruleId: Long): Flow<List<BlockedApp>> =
        blockedAppDao.observeForRule(ruleId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getBlockedApps(ruleId: Long): List<BlockedApp> =
        blockedAppDao.getForRuleSync(ruleId).map { it.toDomain() }

    override suspend fun setBlockedApps(ruleId: Long, apps: List<BlockedApp>) {
        blockedAppDao.deleteAllForRule(ruleId)
        blockedAppDao.insertAll(apps.map { it.copy(ruleId = ruleId).toEntity() })
    }
}
