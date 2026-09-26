package com.salar.focuslock.permissions

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.salar.focuslock.blocking.accessibility.FocusLockAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only check of whether FocusLockAccessibilityService is currently enabled in system
 * Accessibility settings.
 *
 * Deliberately does nothing else: it never enables the service, never opens system settings
 * on its own initiative, and never attempts to work around the user declining or disabling it.
 * The guided "go grant this" UI (deep-linking to Settings.ACTION_ACCESSIBILITY_SETTINGS) is
 * explicitly UI work for a later batch — this class only answers "is it on right now".
 */
@Singleton
class AccessibilityPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "${context.packageName}/${FocusLockAccessibilityService::class.java.name}"

        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServicesSetting)

        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponent, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
