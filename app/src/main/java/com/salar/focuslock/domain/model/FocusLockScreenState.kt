package com.salar.focuslock.domain.model

import java.time.Duration
import java.time.LocalTime

/**
 * Everything the Focus Lock screen needs to render, already resolved from a FocusRule and the
 * current time by FocusLockScreenStateCalculator.
 *
 * Deliberately holds no icon/Drawable (kept Android-free like the other domain models — see
 * InstalledApp's KDoc for the same reasoning). The app icon, when resolved, is loaded
 * separately by the UI layer via AppIconProvider.
 */
data class FocusLockScreenState(
    val packageName: String,
    val appName: String,
    val ruleName: String,
    val remainingDuration: Duration,
    val unlockAt: LocalTime,
    val isStillActive: Boolean
)
