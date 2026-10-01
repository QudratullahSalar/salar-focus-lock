package com.salar.focuslock.blocking.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.salar.focuslock.blocking.coordinator.BlockingCoordinator
import com.salar.focuslock.domain.blocking.BlockingEngine
import dagger.hilt.android.AndroidEntryPoint
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
 * LATENCY FIX: this used to dispatch evaluate() onto a background coroutine. BlockingEngine is
 * now synchronous, in-memory-only work (see its KDoc on RuleSnapshotProvider/RuleCache), so it
 * runs directly on the thread the accessibility event arrives on — no coroutine
 * dispatch/suspension in between. That extra hop was part of what produced a visible
 * flicker/loop (the blocked app briefly foreground and interactive) reported after Batch 4.
 *
 * KNOWN RISK (documented in Batch 2, unchanged here): @AndroidEntryPoint on an
 * AccessibilityService subclass (rather than a direct Service subclass) is the highest-risk
 * Hilt item in this project to verify on a real build. Static inspection has found nothing
 * indicating an actual problem (and this app's real-device testing has since confirmed the
 * service does fire and field injection does work), so this has not been redesigned.
 */
@AndroidEntryPoint
class FocusLockAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var blockingEngine: BlockingEngine

    @Inject
    lateinit var blockingCoordinator: BlockingCoordinator

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        if (foregroundPackage == packageName) return // never evaluate/redirect on ourselves

        val decision = blockingEngine.evaluate(
            packageName = foregroundPackage,
            currentDateTime = LocalDateTime.now(ZoneId.systemDefault())
        )

        // Requirement #8 (Batch 3): the coordinator is only ever invoked for a blocking
        // decision — it never has to branch on shouldBlock itself.
        if (decision.shouldBlock) {
            blockingCoordinator.handleDecision(decision)
        }
    }

    override fun onInterrupt() {
        // Required override. Nothing to tear down here — this service holds no ongoing state.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Event mask, feedback type, and canRetrieveWindowContent are all declared statically
        // in res/xml/accessibility_service_config.xml via the manifest's <meta-data>, so no
        // AccessibilityServiceInfo needs to be constructed or set at runtime here.
    }
}
