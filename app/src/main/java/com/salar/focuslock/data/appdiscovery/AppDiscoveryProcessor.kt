package com.salar.focuslock.data.appdiscovery

import com.salar.focuslock.domain.model.InstalledApp

/**
 * Pure post-processing of a raw InstalledApp list: excludes this app itself, de-duplicates by
 * package name, and sorts by display name. Deliberately has zero Android/PackageManager
 * dependency so it can be unit-tested directly (see AppDiscoveryProcessorTest) without
 * Robolectric or an instrumented device — the PackageManager-dependent part of app discovery
 * (AppDiscoveryRepositoryImpl) is reduced to "build the raw list", and everything about how
 * that list is cleaned up lives here where it's cheaply testable.
 */
object AppDiscoveryProcessor {
    fun process(apps: List<InstalledApp>, selfPackageName: String): List<InstalledApp> =
        apps.filterNot { it.packageName == selfPackageName }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
}
