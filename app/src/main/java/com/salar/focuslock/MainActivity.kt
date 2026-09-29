package com.salar.focuslock

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.salar.focuslock.ui.editor.RuleEditorViewModel
import com.salar.focuslock.ui.main.AppRoot
import com.salar.focuslock.ui.main.MainViewModel
import com.salar.focuslock.ui.theme.SalarFocusLockTheme
import dagger.hilt.android.AndroidEntryPoint

/** Host for the rule list / rule editor screens (see ui/main/AppRoot.kt). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val editorViewModel: RuleEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SalarFocusLockTheme {
                AppRoot(
                    mainViewModel = mainViewModel,
                    editorViewModel = editorViewModel,
                    onOpenAccessibilitySettings = { openAccessibilitySettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have just toggled the service in system settings — re-check on return.
        mainViewModel.refreshAccessibilityStatus()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            // Extremely rare (OEM without the standard settings screen) — nothing sensible to do.
        }
    }
}
