package com.salar.focuslock.permissions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.salar.focuslock.blocking.admin.FocusLockDeviceAdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only check of whether Salar Focus Lock is currently an active Device Administrator (the
 * uninstall-friction feature — see FocusLockDeviceAdminReceiver's KDoc for exactly what that
 * does and does not mean). Mirrors AccessibilityPermissionChecker: reports status only, never
 * enables anything on its own.
 */
@Singleton
class DeviceAdminPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val adminComponentName: ComponentName = ComponentName(context, FocusLockDeviceAdminReceiver::class.java)

    fun isDeviceAdminActive(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return dpm.isAdminActive(adminComponentName)
    }
}
