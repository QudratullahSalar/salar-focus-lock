package com.salar.focuslock.domain.rules

import com.salar.focuslock.domain.model.InstalledApp

/**
 * Which apps the picker shows. System apps (Settings, Phone, ...) are hidden by default so the
 * user can't easily lock themselves out of essential functions, but an already-selected app is
 * always shown so it can be un-selected.
 */
object AppPickerFilter {
    fun filter(
        apps: List<InstalledApp>,
        query: String,
        showSystemApps: Boolean,
        selectedPackages: Set<String>
    ): List<InstalledApp> {
        val q = query.trim()
        return apps.filter { app ->
            val visibleByType = showSystemApps || !app.isSystemApp || app.packageName in selectedPackages
            val matchesQuery = q.isEmpty() ||
                app.appName.contains(q, ignoreCase = true) ||
                app.packageName.contains(q, ignoreCase = true)
            visibleByType && matchesQuery
        }
    }
}
