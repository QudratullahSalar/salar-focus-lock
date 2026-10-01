package com.salar.focuslock

import android.app.Application
import com.salar.focuslock.data.repository.RuleCache
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * Application entry point. Hilt-annotated so the DI graph (Room database, DataStore,
 * repositories) is available from app startup.
 *
 * Also starts RuleCache here — once, for the whole process lifetime — so BlockingEngine's
 * in-memory snapshot (see RuleCache's KDoc) is already warm by the time the
 * AccessibilityService's first event arrives, rather than starting cold on first use.
 */
@HiltAndroidApp
class FocusLockApp : Application() {

    @Inject
    lateinit var ruleCache: RuleCache

    // Lives for the whole process — there's nothing to cancel it on Application teardown, since
    // the OS simply kills the process (standard practice for an app-wide background mirror).
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ruleCache.start(applicationScope)
    }
}
