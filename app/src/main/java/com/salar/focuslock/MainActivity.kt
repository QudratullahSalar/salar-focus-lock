package com.salar.focuslock

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.salar.focuslock.permissions.DeviceAdminPermissionChecker
import com.salar.focuslock.ui.editor.RuleEditorViewModel
import com.salar.focuslock.ui.main.AppRoot
import com.salar.focuslock.ui.main.MainViewModel
import com.salar.focuslock.ui.theme.SalarFocusLockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Host for the rule list / rule editor screens (see ui/main/AppRoot.kt). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val editorViewModel: RuleEditorViewModel by viewModels()

    @Inject
    lateinit var deviceAdminPermissionChecker: DeviceAdminPermissionChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SalarFocusLockTheme {
                AppRoot(
                    mainViewModel = mainViewModel,
                    editorViewModel = editorViewModel,
                    onOpenAccessibilitySettings = { openAccessibilitySettings() },
                    onRequestDeviceAdmin = { requestDeviceAdmin() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have just toggled either setting in system screens — re-check on return.
        mainViewModel.refreshAccessibilityStatus()
        mainViewModel.refreshDeviceAdminStatus()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            // Extremely rare (OEM without the standard settings screen) — nothing sensible to do.
        }
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminPermissionChecker.adminComponentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Requires deactivating admin access here before Salar Focus Lock can be " +
                    "uninstalled. Grants no other control over this device."
            )
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Extremely rare (OEM without the standard device-admin screen) — nothing sensible to do.
        }
    }
}
