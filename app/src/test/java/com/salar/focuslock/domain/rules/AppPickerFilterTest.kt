package com.salar.focuslock.domain.rules

import com.salar.focuslock.domain.model.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Written but NOT executed locally (no JVM/Gradle in the authoring environment). */
class AppPickerFilterTest {

    private fun app(pkg: String, name: String, system: Boolean = false) =
        InstalledApp(packageName = pkg, appName = name, isSystemApp = system, isLaunchable = true)

    private val tiktok = app("com.zhiliaoapp.musically", "TikTok")
    private val youtube = app("com.google.android.youtube", "YouTube")
    private val settings = app("com.android.settings", "Settings", system = true)
    private val all = listOf(tiktok, youtube, settings)

    @Test
    fun `system apps are hidden by default`() {
        val result = AppPickerFilter.filter(all, "", showSystemApps = false, selectedPackages = emptySet())
        assertEquals(listOf(tiktok, youtube), result)
    }

    @Test
    fun `system apps are shown when enabled`() {
        val result = AppPickerFilter.filter(all, "", showSystemApps = true, selectedPackages = emptySet())
        assertEquals(all, result)
    }

    @Test
    fun `an already selected system app stays visible even when system apps are hidden`() {
        val result = AppPickerFilter.filter(
            all, "", showSystemApps = false, selectedPackages = setOf("com.android.settings")
        )
        assertEquals(listOf(tiktok, youtube, settings), result)
    }

    @Test
    fun `query matches app name case-insensitively`() {
        val result = AppPickerFilter.filter(all, "tikt", showSystemApps = false, selectedPackages = emptySet())
        assertEquals(listOf(tiktok), result)
    }

    @Test
    fun `query also matches package name`() {
        val result = AppPickerFilter.filter(all, "google", showSystemApps = false, selectedPackages = emptySet())
        assertEquals(listOf(youtube), result)
    }

    @Test
    fun `blank or whitespace query returns everything visible`() {
        val result = AppPickerFilter.filter(all, "   ", showSystemApps = false, selectedPackages = emptySet())
        assertEquals(listOf(tiktok, youtube), result)
    }

    @Test
    fun `no match returns an empty list`() {
        val result = AppPickerFilter.filter(all, "zzz", showSystemApps = true, selectedPackages = emptySet())
        assertEquals(emptyList<InstalledApp>(), result)
    }
}
