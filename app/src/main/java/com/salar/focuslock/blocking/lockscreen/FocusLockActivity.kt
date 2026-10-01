package com.salar.focuslock.blocking.lockscreen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.salar.focuslock.data.appdiscovery.AppIconProvider
import com.salar.focuslock.domain.model.BlockDecision
import com.salar.focuslock.domain.repository.AppDiscoveryRepository
import com.salar.focuslock.domain.repository.RuleRepository
import com.salar.focuslock.ui.theme.SalarFocusLockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The Focus Lock redirect screen's host Activity. Contains no business/scheduling logic itself
 * — it reads two extras (which package, which rule), and FocusLockRoute/FocusLockScreenStateCalculator
 * do the rest, re-reading the rule from RuleRepository and recomputing from the current time
 * on every tick (see FocusLockScreenStateCalculator's KDoc on requirement #5).
 *
 * Only launched by BlockingCoordinator, only ever with a [BlockDecision] where shouldBlock was
 * true — see [createIntent]. Receives only packageName + ruleId as extras, nothing from the
 * blocked app's own screen/content (there is nothing to receive: AccessibilityService never
 * reads that content in the first place).
 */
@AndroidEntryPoint
class FocusLockActivity : ComponentActivity() {

    @Inject lateinit var ruleRepository: RuleRepository
    @Inject lateinit var appDiscoveryRepository: AppDiscoveryRepository
    @Inject lateinit var appIconProvider: AppIconProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Never let the system Back gesture/button return the user to the blocked app. Sending
        // the task to background (revealing whatever was behind it — typically the home
        // screen, since this Activity is launched with FLAG_ACTIVITY_NEW_TASK) is the
        // legitimate, documented way to do this; it does not disable Back system-wide and does
        // not claim to be unbypassable beyond this screen itself.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goHome()
                }
            }
        )

        renderFromIntent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTask launch mode (see the manifest entry) means a second block redirect for a
        // DIFFERENT package while this screen is already showing reuses this instance via
        // onNewIntent rather than creating a duplicate — recreate() is the simplest safe way to
        // get Compose to pick up the new extras. This is an uncommon path (rapidly switching
        // between two different blocked apps) and the brief recreation flicker is an acceptable
        // trade-off for correctness at this batch's scope.
        setIntent(intent)
        recreate()
    }

    private fun renderFromIntent() {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val ruleId = intent.getLongExtra(EXTRA_RULE_ID, NO_RULE_ID)

        if (packageName == null || ruleId == NO_RULE_ID) {
            // Malformed launch — shouldn't happen via BlockingCoordinator.createIntent, but
            // never show a broken lock screen; just don't block.
            finish()
            return
        }

        setContent {
            SalarFocusLockTheme {
                FocusLockRoute(
                    packageName = packageName,
                    ruleId = ruleId,
                    ruleRepository = ruleRepository,
                    appDiscoveryRepository = appDiscoveryRepository,
                    appIconProvider = appIconProvider,
                    onShouldClose = { goHome() }
                )
            }
        }
    }

    /**
     * BUG FIX: this used to call moveTaskToBack(true). That was wrong — FocusLockActivity is
     * launched with FLAG_ACTIVITY_NEW_TASK from the moment the BLOCKED app is foreground, so
     * the task sitting immediately underneath this one is the blocked app's own task, not the
     * home screen. moveTaskToBack() just reveals whatever is underneath, which meant it
     * revealed the blocked app itself — briefly interactive — which AccessibilityService then
     * detected again and re-triggered this same screen, producing exactly the flicker/loop
     * reported: exit, blocked app flashes for a moment, re-lock, repeat.
     *
     * Explicitly launching the home screen's own intent (and finishing this Activity) always
     * goes to Home, never back into the task underneath, which is the fix.
     */
    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE_NAME = "com.salar.focuslock.extra.PACKAGE_NAME"
        private const val EXTRA_RULE_ID = "com.salar.focuslock.extra.RULE_ID"
        private const val NO_RULE_ID = -1L

        /**
         * Precondition: [decision].shouldBlock must be true (ruleId is non-null in that case —
         * see BlockDecision's KDoc). Only BlockingCoordinator calls this.
         */
        fun createIntent(context: Context, decision: BlockDecision): Intent {
            require(decision.shouldBlock) { "createIntent requires a blocking BlockDecision" }
            val ruleId = requireNotNull(decision.ruleId) { "shouldBlock=true implies ruleId is non-null" }

            return Intent(context, FocusLockActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, decision.packageName)
                putExtra(EXTRA_RULE_ID, ruleId)
                // NEW_TASK: required to start an Activity from a non-Activity Context (the
                // BlockingCoordinator/Service). CLEAR_TOP + SINGLE_TOP together with the
                // manifest's singleTask launch mode prevent stacking duplicate instances.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
    }
}
