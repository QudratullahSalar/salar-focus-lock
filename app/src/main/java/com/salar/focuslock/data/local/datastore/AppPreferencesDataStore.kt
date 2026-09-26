package com.salar.focuslock.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "salar_focus_lock_prefs")

/**
 * Lightweight app-level preferences — not focus-rule data (that's Room). Covers only what's
 * needed so far: notification toggle, onboarding flag, theme, and the Advanced (Device Owner)
 * mode flag, which future batches read to decide whether to show the Advanced Mode setup flow.
 */
@Singleton
class AppPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val ADVANCED_MODE_ENABLED = booleanPreferencesKey("advanced_mode_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val onboardingComplete: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val advancedModeEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ADVANCED_MODE_ENABLED] ?: false }

    /** One of "system", "light", "dark". */
    val themeMode: Flow<String> =
        context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setAdvancedModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ADVANCED_MODE_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }
}
