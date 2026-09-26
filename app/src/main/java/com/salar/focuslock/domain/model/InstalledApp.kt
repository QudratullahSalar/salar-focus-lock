package com.salar.focuslock.domain.model

/**
 * A launchable installed application, as surfaced by the app-picker.
 *
 * Deliberately holds no icon/Drawable reference: keeping this model free of android.graphics
 * types keeps the domain layer Android-framework-free and directly unit-testable (see
 * AppDiscoveryProcessorTest). Icon bitmaps are fetched on demand from the data layer
 * (AppDiscoveryRepositoryImpl.getAppIcon) by whatever UI needs to render them, the same way
 * BlockedApp already caches an iconUriCache string rather than a live Drawable.
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isLaunchable: Boolean
)
