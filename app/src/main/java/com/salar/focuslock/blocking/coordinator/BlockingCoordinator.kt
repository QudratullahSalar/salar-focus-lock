package com.salar.focuslock.blocking.coordinator

import android.content.Context
import android.os.SystemClock
import com.salar.focuslock.blocking.lockscreen.FocusLockActivity
import com.salar.focuslock.domain.model.BlockDecision
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sits between BlockingEngine's decision and the actual UI redirect:
 *
 *   AccessibilityService -> BlockingEngine -> BlockDecision -> BlockingCoordinator -> FocusLockActivity
 *
 * All this class does is decide *whether to launch* the Focus Lock screen right now and then
 * launch it — no rule evaluation, no navigation/composable logic (that lives in
 * FocusLockActivity/FocusLockScreen). This keeps BlockingEngine itself free of any
 * Android/UI concern, per requirement #3.
 *
 * Precondition: callers only invoke [handleDecision] for a [BlockDecision] where
 * [BlockDecision.shouldBlock] is true — FocusLockAccessibilityService enforces that gate (see
 * its KDoc / requirement #8). This class does not itself re-check shouldBlock.
 */
@Singleton
class BlockingCoordinator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Debounce state. A blocked app fires MANY TYPE_WINDOW_STATE_CHANGED events for internal
    // navigation (e.g. switching tabs within TikTok) without ever leaving the foreground
    // package, and each one reaches BlockingEngine -> a fresh "shouldBlock=true" decision. This
    // debounce exists purely to avoid redundant startActivity() calls/flicker in that burst —
    // it is NOT the actual loop/duplicate-instance protection (that's FocusLockActivity's
    // singleTask launch mode, and the fact that once the lock screen has the foreground,
    // FocusLockAccessibilityService's self-package check stops any further evaluation — see its
    // KDoc). A short, fixed window is deliberately used rather than any open-ended cooldown, so
    // it never delays legitimate (re-)blocking.
    @Volatile private var lastLaunchedPackage: String? = null
    @Volatile private var lastLaunchAtElapsedRealtime: Long = 0L

    fun handleDecision(decision: BlockDecision) {
        val now = SystemClock.elapsedRealtime()
        val recentlyLaunchedSamePackage = decision.packageName == lastLaunchedPackage &&
            (now - lastLaunchAtElapsedRealtime) < DEBOUNCE_WINDOW_MILLIS
        if (recentlyLaunchedSamePackage) return

        lastLaunchedPackage = decision.packageName
        lastLaunchAtElapsedRealtime = now

        val intent = FocusLockActivity.createIntent(context, decision)
        context.startActivity(intent)
    }

    companion object {
        private const val DEBOUNCE_WINDOW_MILLIS = 1_500L
    }
}
