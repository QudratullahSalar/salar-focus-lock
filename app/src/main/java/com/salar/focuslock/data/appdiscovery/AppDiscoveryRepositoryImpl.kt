package com.salar.focuslock.data.appdiscovery

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import com.salar.focuslock.domain.model.InstalledApp
import com.salar.focuslock.domain.repository.AppDiscoveryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PackageManager-backed AppDiscoveryRepository. All PackageManager access is confined to this
 * class — the domain layer and anything consuming AppDiscoveryRepository only ever sees
 * InstalledApp / the interface.
 */
@Singleton
class AppDiscoveryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppDiscoveryRepository, AppIconProvider {

    private val packageManager: PackageManager get() = context.packageManager
    private val selfPackageName: String get() = context.packageName

    override suspend fun getInstalledLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedActivities: List<ResolveInfo> = try {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            // Defensive: some OEM PackageManager implementations throw under odd conditions
            // (corrupted package cache, etc.) — degrade to an empty list rather than crash.
            emptyList()
        }

        val rawApps = resolvedActivities.mapNotNull { it.toInstalledAppOrNull() }
        AppDiscoveryProcessor.process(rawApps, selfPackageName)
    }

    override suspend fun getAppInfo(packageName: String): InstalledApp? = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            InstalledApp(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                isSystemApp = appInfo.isNonUpdatedSystemApp(),
                // Best-effort assumption for a single lookup — unlike getInstalledLaunchableApps,
                // this isn't reached via a resolved LAUNCHER query, so launchability isn't
                // independently verified here. Acceptable for the Focus Lock screen's use case
                // (displaying info about an app we already know was foregrounded moments ago).
                isLaunchable = true
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Uninstalled/updated since it was detected as foreground — no crash, just no info.
            null
        }
    }

    /**
     * On-demand icon lookup for a single package (AppIconProvider). Kept out of
     * AppDiscoveryRepository itself — icons are deliberately kept out of the domain model, see
     * InstalledApp's KDoc.
     */
    override suspend fun getIcon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            // Uninstalled/updated since the caller last saw this package — no crash, just no icon.
            null
        }
    }

    private fun ResolveInfo.toInstalledAppOrNull(): InstalledApp? {
        val pkgName = activityInfo?.packageName ?: return null
        return try {
            val appInfo: ApplicationInfo = packageManager.getApplicationInfo(pkgName, 0)
            InstalledApp(
                packageName = pkgName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                isSystemApp = appInfo.isNonUpdatedSystemApp(),
                // Reached via a resolved ACTION_MAIN/CATEGORY_LAUNCHER query, so launchable by definition.
                isLaunchable = true
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Uninstalled/updated between the queryIntentActivities() call and this lookup —
            // skip it rather than crash the whole discovery pass.
            null
        }
    }
}

/**
 * True for preinstalled system apps that the user has NOT updated. A preinstalled app that has
 * received a Play Store update (YouTube, Chrome, Gmail...) carries FLAG_UPDATED_SYSTEM_APP and is
 * treated as a normal user-facing app, so it isn't hidden by the picker's "system apps" filter.
 */
private fun ApplicationInfo.isNonUpdatedSystemApp(): Boolean =
    (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
        (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
