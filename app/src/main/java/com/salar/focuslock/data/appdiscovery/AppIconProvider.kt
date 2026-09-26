package com.salar.focuslock.data.appdiscovery

import android.graphics.drawable.Drawable

/**
 * Deliberately NOT in domain/repository like AppDiscoveryRepository — a Drawable is an Android
 * framework type, so this interface is UI-facing PackageManager glue rather than part of the
 * Android-free domain layer. Still a separate interface (implemented by
 * AppDiscoveryRepositoryImpl) rather than injecting the concrete class directly, so
 * FocusLockActivity depends on an abstraction here too.
 */
interface AppIconProvider {
    /** Null if [packageName] can no longer be resolved (uninstalled/updated). Never throws. */
    suspend fun getIcon(packageName: String): Drawable?
}
