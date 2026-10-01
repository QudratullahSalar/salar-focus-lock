package com.salar.focuslock.blocking.admin

import android.app.admin.DeviceAdminReceiver

/**
 * Registers Salar Focus Lock as a plain (non-owner) Device Administrator. This is NOT Device
 * Owner mode — that remains explicitly out of scope. It requests zero management policies (see
 * res/xml/device_admin_receiver.xml).
 *
 * Its only real effect is the standard Android behavior that applies to ANY active device
 * admin: before this app can be uninstalled, the user must first deactivate it here
 * (Settings > Security > Device admin apps). That is a deliberate extra step, not a technical
 * block — anyone who wants to uninstall the app always still can, by deactivating admin first.
 * This is never described as anything stronger than that, anywhere in this app's UI or code —
 * see the "Uninstall protection" card in RuleListScreen for the exact wording shown to the user.
 */
class FocusLockDeviceAdminReceiver : DeviceAdminReceiver()
