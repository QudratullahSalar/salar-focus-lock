package com.salar.focuslock.blocking.lockscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.salar.focuslock.data.appdiscovery.AppIconProvider
import com.salar.focuslock.domain.blocking.FocusLockScreenStateCalculator
import com.salar.focuslock.domain.model.FocusLockScreenState
import com.salar.focuslock.domain.repository.AppDiscoveryRepository
import com.salar.focuslock.domain.repository.RuleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Owns the "recompute from scratch every second, from the real repositories" loop described in
 * FocusLockScreenStateCalculator's KDoc, and the one-time app-name/icon lookup. Delegates all
 * rendering to the stateless FocusLockScreen composable — this function is the only place with
 * any orchestration logic, keeping FocusLockScreen itself trivially previewable/testable-by-eye
 * and FocusLockActivity itself free of business logic (requirement #3/#4).
 */
@Composable
fun FocusLockRoute(
    packageName: String,
    ruleId: Long,
    ruleRepository: RuleRepository,
    appDiscoveryRepository: AppDiscoveryRepository,
    appIconProvider: AppIconProvider,
    onShouldClose: () -> Unit
) {
    var appName by remember(packageName) { mutableStateOf(packageName) }
    var appIcon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    var screenState by remember(packageName, ruleId) { mutableStateOf<FocusLockScreenState?>(null) }

    // App name/icon are resolved once — they don't change while this screen is up, unlike the
    // remaining-time state below which must keep recomputing.
    LaunchedEffect(packageName) {
        appName = appDiscoveryRepository.getAppInfo(packageName)?.appName ?: packageName
    }
    LaunchedEffect(packageName) {
        appIcon = try {
            appIconProvider.getIcon(packageName)?.toBitmap()?.asImageBitmap()
        } catch (e: Exception) {
            // Defensive: an unusual Drawable (e.g. a huge/degenerate one) failing to rasterize
            // should never crash the lock screen — just show it without an app icon.
            null
        }
    }

    // The actual source of truth: re-fetch the rule and recompute against "now" every second.
    // Never trusts a retained countdown value — see requirement #5 and the calculator's KDoc.
    LaunchedEffect(ruleId) {
        while (isActive) {
            val rule = ruleRepository.getRuleById(ruleId)
            val state = FocusLockScreenStateCalculator.calculate(
                rule = rule,
                packageName = packageName,
                appName = appName,
                currentDateTime = LocalDateTime.now(ZoneId.systemDefault())
            )
            screenState = state

            if (state == null || !state.isStillActive) {
                // Rule was deleted, disabled, or its window has simply ended since the redirect
                // was launched — nothing left to enforce, let the user proceed.
                onShouldClose()
                break
            }
            delay(1_000L)
        }
    }

    val currentState = screenState
    if (currentState != null) {
        FocusLockScreen(state = currentState, appIcon = appIcon)
    }
    // else: nothing rendered yet for the first ~one coroutine dispatch — imperceptibly brief,
    // and preferable to a flash of a wrong/placeholder remaining time.
}
