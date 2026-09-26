package com.salar.focuslock.blocking.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.salar.focuslock.blocking.coordinator.BlockingCoordinator
import com.salar.focuslock.domain.blocking.BlockingEngine
import com.salar.focuslock.domain.model.BlockDecision
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Detects which app is currently in the foreground and asks BlockingEngine whether it should
 * be blocked right now. Contains no business logic itself — see BlockingEngine's KDoc for the
 * full pipeline this sits at the top of.
 *
 * Privacy: this service reads only TYPE_WINDOW_STATE_CHANGED events and the package name
 * attached to them (AccessibilityEvent.packageName). It never reads screen text, passwords,
 * messages, notification content, or clipboard data, and canRetrieveWindowContent is set to
 * false in res/xml/accessibility_service_config.xml, so window content is not even available
 * to this class even if a future change accidentally tried to read it. Nothing this service
 * observes is written to disk, logged with content, or transmitted anywhere.
 *
 * Batch 3: detection + decision + redirect. When BlockDecision.shouldBlock is true, this hands
 * off to BlockingCoordinator, which owns the actual redirect (launching FocusLockActivity) —
 * see BlockingCoordinator's KDoc. This class still does no navigation/UI work itself, per
 * requirement #8.
 *
 * KNOWN RISK (documented in Batch 2, unchanged here): @AndroidEntryPoint on an
 * AccessibilityService subclass (rather than a direct Service subclass) is the highest-risk
 * Hilt item in this project to verify on the first real Android build. Static inspection this
 * batch found nothing indicating an actual problem, so this has not been redesigned — flagging
 * it again here per requirement #12 rather than silently working around it.
 */
@AndroidEntryPoint
class FocusLockAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var blockingEngine: BlockingEngine

    @Inject
    lateinit var blockingCoordinator: BlockingCoordinator

    // SupervisorJob so one failed evaluation doesn't cancel the whole service's coroutine
    // scope; Dispatchers.Default because BlockingEngine's work is CPU-bound repository/rule
    // evaluation, not I/O (Room's own suspend functions already move to their own dispatcher).
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        if (foregroundPackage == packageName) return // never evaluate/redirect on ourselves

        serviceScope.launch {
            val decision = blockingEngine.evaluate(
                packageName = foregroundPackage,
                currentDateTime = LocalDateTime.now(ZoneId.systemDefault())
            )
            handleDecision(decision)
        }
    }

    private fun handleDecision(decision: BlockDecision) {
        // Requirement #8: the coordinator is only ever invoked for a blocking decision — it is
        // never called (and never has to branch on shouldBlock itself) otherwise.
        if (decision.shouldBlock) {
            blockingCoordinator.handleDecision(decision)
        }
    }

    override fun onInterrupt() {
        // Required override. No ongoing state needs cleanup here specifically; teardown
        // happens in onDestroy().
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Event mask, feedback type, and canRetrieveWindowContent are all declared statically
        // in res/xml/accessibility_service_config.xml via the manifest's <meta-data>, so no
        // AccessibilityServiceInfo needs to be constructed or set at runtime here.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
