package com.salar.focuslock.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salar.focuslock.data.appdiscovery.AppIconProvider
import com.salar.focuslock.domain.model.BlockedApp
import com.salar.focuslock.domain.model.FocusRule
import com.salar.focuslock.domain.model.InstalledApp
import com.salar.focuslock.domain.repository.AppDiscoveryRepository
import com.salar.focuslock.domain.repository.RuleRepository
import com.salar.focuslock.domain.rules.RuleDraft
import com.salar.focuslock.domain.rules.RuleDraftError
import com.salar.focuslock.domain.scheduler.ScheduleEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/** State + actions for the create/edit rule screen. */
@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val appDiscoveryRepository: AppDiscoveryRepository,
    private val appIconProvider: AppIconProvider
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val ruleId: Long? = null,
        val draft: RuleDraft = RuleDraft(),
        val installedApps: List<InstalledApp> = emptyList(),
        val searchQuery: String = "",
        val showSystemApps: Boolean = false,
        val validationError: RuleDraftError? = null,
        val isLockedForEditing: Boolean = false,
        val saveFailed: Boolean = false,
        val isSaved: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Which rule is loaded. Makes start() idempotent, so a screen rotation doesn't reload the
    // form and throw away what the user already typed.
    private var loadedKey: Long? = null

    private val iconCache = HashMap<String, ImageBitmap?>()

    fun start(ruleId: Long?) {
        val key = ruleId ?: NEW_RULE_KEY
        if (loadedKey == key) return
        loadedKey = key
        _state.value = UiState(isLoading = true, ruleId = ruleId)

        viewModelScope.launch {
            val apps = appDiscoveryRepository.getInstalledLaunchableApps()
            val existing = ruleId?.let { ruleRepository.getRuleById(it) }
            val existingApps = if (existing != null) ruleRepository.getBlockedApps(existing.id) else emptyList()
            if (loadedKey != key) return@launch // editor was closed/reopened meanwhile

            _state.update {
                it.copy(
                    isLoading = false,
                    installedApps = apps,
                    draft = if (existing != null) RuleDraft.from(existing, existingApps) else RuleDraft(),
                    isLockedForEditing = existing != null && ScheduleEngine.isRuleActive(existing, now())
                )
            }
        }
    }

    /** Resets the editor (called when leaving the screen). */
    fun clear() {
        loadedKey = null
        _state.value = UiState()
    }

    fun setName(name: String) = editDraft { it.copy(name = name) }
    fun setStart(minuteOfDay: Int) = editDraft { it.copy(startMinuteOfDay = minuteOfDay) }
    fun setEnd(minuteOfDay: Int) = editDraft { it.copy(endMinuteOfDay = minuteOfDay) }

    fun toggleDay(day: DayOfWeek) = editDraft {
        it.copy(repeatDays = if (day in it.repeatDays) it.repeatDays - day else it.repeatDays + day)
    }

    fun toggleApp(app: InstalledApp) = editDraft {
        val selected = it.selectedApps
        it.copy(
            selectedApps = if (app.packageName in selected) selected - app.packageName
            else selected + (app.packageName to app.appName)
        )
    }

    fun setSearchQuery(query: String) = _state.update { it.copy(searchQuery = query) }
    fun setShowSystemApps(show: Boolean) = _state.update { it.copy(showSystemApps = show) }

    fun save() {
        val current = _state.value
        if (current.isLoading || current.isLockedForEditing) return

        val error = current.draft.validate()
        if (error != null) {
            _state.update { it.copy(validationError = error) }
            return
        }

        viewModelScope.launch {
            try {
                val draft = current.draft
                val existing = current.ruleId?.let { ruleRepository.getRuleById(it) }
                if (existing != null && ScheduleEngine.isRuleActive(existing, now())) {
                    _state.update { it.copy(isLockedForEditing = true) }
                    return@launch
                }

                val timestamp = Instant.now()
                val rule = existing?.copy(
                    name = draft.name.trim(),
                    startMinuteOfDay = draft.startMinuteOfDay,
                    endMinuteOfDay = draft.endMinuteOfDay,
                    repeatDays = draft.repeatDays,
                    updatedAt = timestamp
                ) ?: FocusRule(
                    name = draft.name.trim(),
                    startMinuteOfDay = draft.startMinuteOfDay,
                    endMinuteOfDay = draft.endMinuteOfDay,
                    repeatDays = draft.repeatDays,
                    isEnabled = true,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )

                val savedId = ruleRepository.upsertRule(rule)
                ruleRepository.setBlockedApps(
                    savedId,
                    draft.selectedApps.map { (pkg, label) ->
                        BlockedApp(ruleId = savedId, packageName = pkg, appLabelCache = label)
                    }
                )
                _state.update { it.copy(isSaved = true, saveFailed = false) }
            } catch (e: Exception) {
                _state.update { it.copy(saveFailed = true) }
            }
        }
    }

    /** Small (96px) icons, cached — full-size adaptive icons for a long list could exhaust memory. */
    suspend fun loadIcon(packageName: String): ImageBitmap? {
        if (iconCache.containsKey(packageName)) return iconCache[packageName]
        val icon = try {
            appIconProvider.getIcon(packageName)?.toBitmap(width = 96, height = 96)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
        iconCache[packageName] = icon
        return icon
    }

    private fun editDraft(transform: (RuleDraft) -> RuleDraft) {
        _state.update {
            if (it.isLockedForEditing) it
            else it.copy(draft = transform(it.draft), validationError = null, saveFailed = false)
        }
    }

    private fun now(): LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())

    private companion object {
        const val NEW_RULE_KEY = -1L
    }
}
