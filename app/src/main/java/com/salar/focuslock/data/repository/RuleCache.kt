package com.salar.focuslock.data.repository

import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.repository.RuleRepository
import com.salar.focuslock.domain.repository.RuleSnapshotProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps an in-memory mirror of RuleRepository's rules + their blocked apps, refreshed
 * reactively whenever either changes, so BlockingEngine's per-AccessibilityService-event
 * evaluation (see RuleSnapshotProvider's KDoc) is pure in-memory list filtering — no
 * suspend/Room round trip on that path.
 *
 * This exists specifically to shrink the window between "blocked app becomes foreground" and
 * "Focus Lock screen takes over": every suspend/DB hop in that path is a few extra
 * milliseconds the blocked app's own UI is visible and technically interactive. It is NOT a
 * complete fix for that window — some single compositor frame of the blocked app can still be
 * visible before the redirect Activity's window actually draws, since Android does not offer
 * any API to make a foreground-app transition instantaneous for a normal (non-Device-Owner)
 * app. What this does fix is any *additional* delay beyond that unavoidable platform floor.
 */
@Singleton
class RuleCache @Inject constructor(
    private val ruleRepository: RuleRepository
) : RuleSnapshotProvider {

    @Volatile private var snapshot: List<Pair<FocusRule, List<BlockedApp>>> = emptyList()
    @Volatile private var started = false

    /**
     * Starts mirroring RuleRepository into memory, for as long as [scope] lives. Call once
     * (from FocusLockApp.onCreate(), with an application-scoped CoroutineScope) — safe to call
     * more than once, later calls are a no-op. Until the first emission arrives, the snapshot
     * is empty, so getEnabledRulesSnapshot()/getBlockedAppsSnapshot() simply report "nothing
     * blocked yet" rather than throwing — a brief, harmless gap right at process start.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Synchronized
    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        ruleRepository.observeRules()
            .flatMapLatest { rules ->
                if (rules.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        rules.map { rule ->
                            ruleRepository.observeBlockedApps(rule.id).map { apps -> rule to apps }
                        }
                    ) { it.toList() }
                }
            }
            .onEach { snapshot = it }
            .launchIn(scope)
    }

    override fun getEnabledRulesSnapshot(): List<FocusRule> =
        snapshot.filter { it.first.isEnabled }.map { it.first }

    override fun getBlockedAppsSnapshot(ruleId: Long): List<BlockedApp> =
        snapshot.firstOrNull { it.first.id == ruleId }?.second.orEmpty()
}
