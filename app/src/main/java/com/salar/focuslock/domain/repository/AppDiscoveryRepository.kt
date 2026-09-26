package com.salar.focuslock.domain.repository

import com.salar.focuslock.domain.model.InstalledApp

/**
 * Abstraction over "which apps are installed and launchable on this device", so the rest of
 * the app (rule-creation UI, in a later batch) depends on this interface rather than directly
 * on android.content.pm.PackageManager.
 */
interface AppDiscoveryRepository {
    /**
     * Launchable installed apps, excluding Salar Focus Lock itself, deduplicated and sorted
     * by display name. Never throws for an individual package that disappears mid-query
     * (uninstalled/updated concurrently) — that package is simply omitted from the result.
     */
    suspend fun getInstalledLaunchableApps(): List<InstalledApp>

    /**
     * Metadata for a single package, or null if it can no longer be resolved — e.g. the app
     * was uninstalled or updated between when it was blocked and when this is called. Used by
     * the Focus Lock screen so a vanished app is handled safely rather than crashing.
     */
    suspend fun getAppInfo(packageName: String): InstalledApp?
}
