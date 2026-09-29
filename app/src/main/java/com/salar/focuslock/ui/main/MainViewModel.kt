package com.salar.focuslock.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.repository.RuleRepository
import com.salar.focuslock.domain.scheduler.ScheduleEngine
import com.salar.focuslock.permissions.AccessibilityPermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

sealed interface Screen {
    object RuleList : Screen
    data class RuleEditor(val ruleId: Long?) : Screen
}

data class RuleListItem(
    val rule: FocusRule,
    val appLabels: List<String>,
    /** True while the rule is inside its active window — it can't be changed or removed then. */
    val isLockedNow: Boolean
)

/** Rule list + simple navigation + accessibility status. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val accessibilityPermissionChecker: AccessibilityPermissionChecker
) : ViewModel() {

    private val _screen = MutableStateFlow<Screen>(Screen.RuleList)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _accessibilityEnabled =
        MutableStateFlow(accessibilityPermissionChecker.isAccessibilityServiceEnabled())
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    // Re-evaluates "is this rule locked right now" periodically, so the list updates by itself
    // when a rule's window starts or ends while the screen is open.
    private val clock: Flow<LocalDateTime> = flow {
        while (true) {
            emit(now())
            delay(CLOCK_TICK_MILLIS)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rulesWithApps: Flow<List<Pair<FocusRule, List<String>>>> =
        ruleRepository.observeRules().flatMapLatest { rules ->
            if (rules.isEmpty()) {
                flowOf(emptyList<Pair<FocusRule, List<String>>>())
            } else {
                combine(
                    rules.map { rule ->
                        ruleRepository.observeBlockedApps(rule.id)
                            .map { apps -> rule to apps.map { it.appLabelCache } }
                    }
                ) { it.toList() }
            }
        }

    /** null = still loading. */
    val rules: StateFlow<List<RuleListItem>?> =
        combine(rulesWithApps, clock) { list, now ->
            list.map { (rule, labels) ->
                RuleListItem(rule, labels, ScheduleEngine.isRuleActive(rule, now))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    fun refreshAccessibilityStatus() {
        _accessibilityEnabled.value = accessibilityPermissionChecker.isAccessibilityServiceEnabled()
    }

    fun openEditor(ruleId: Long?) {
        _screen.value = Screen.RuleEditor(ruleId)
    }

    fun closeEditor() {
        _screen.value = Screen.RuleList
    }

    fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val current = ruleRepository.getRuleById(ruleId) ?: return@launch
            // An active rule can't be switched off — that would make the lock trivial to bypass.
            if (ScheduleEngine.isRuleActive(current, now())) return@launch
            ruleRepository.upsertRule(current.copy(isEnabled = enabled, updatedAt = Instant.now()))
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            val current = ruleRepository.getRuleById(ruleId) ?: return@launch
            if (ScheduleEngine.isRuleActive(current, now())) return@launch
            ruleRepository.deleteRule(current)
        }
    }

    private fun now(): LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())

    private companion object {
        const val CLOCK_TICK_MILLIS = 15_000L
    }
}
