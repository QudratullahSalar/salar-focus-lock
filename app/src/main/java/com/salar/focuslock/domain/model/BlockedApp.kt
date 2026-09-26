package com.salar.focuslock.domain.model

/**
 * A single installed app selected for blocking under a given [FocusRule].
 * [appLabelCache] / [iconUriCache] are cached copies of PackageManager lookups so the UI
 * doesn't need a live PackageManager query on every render; they're refreshed opportunistically
 * (app-picker open, app update) rather than being a live source of truth.
 */
data class BlockedApp(
    val id: Long = 0L,
    val ruleId: Long,
    val packageName: String,
    val appLabelCache: String,
    val iconUriCache: String? = null
)
