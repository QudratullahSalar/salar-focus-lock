package com.salar.focuslock.data.appdiscovery

import com.salar.focuslock.domain.model.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppDiscoveryProcessor — the pure (no PackageManager) part of app discovery.
 * The PackageManager-dependent part (AppDiscoveryRepositoryImpl.getInstalledLaunchableApps)
 * is not unit-testable without Robolectric or an instrumented device, which is why the
 * post-processing logic was deliberately factored out into this pure function; this is the
 * "tests for AppDiscovery where practical using abstractions" the batch asked for.
 *
 * IMPORTANT: written but NOT executed — no JVM available in this environment. Verified by
 * manual read-through against AppDiscoveryProcessor's logic only.
 */
class AppDiscoveryProcessorTest {

    private fun app(pkg: String, name: String, isSystem: Boolean = false): InstalledApp =
        InstalledApp(packageName = pkg, appName = name, isSystemApp = isSystem, isLaunchable = true)

    @Test
    fun `excludes Salar Focus Lock itself from the result`() {
        val apps = listOf(
            app("com.salar.focuslock", "Salar Focus Lock"),
            app("com.example.tiktok", "TikTok")
        )

        val result = AppDiscoveryProcessor.process(apps, selfPackageName = "com.salar.focuslock")

        assertEquals(1, result.size)
        assertEquals("com.example.tiktok", result.single().packageName)
    }

    @Test
    fun `deduplicates by package name keeping one entry`() {
        val apps = listOf(
            app("com.example.tiktok", "TikTok"),
            app("com.example.tiktok", "TikTok")
        )

        val result = AppDiscoveryProcessor.process(apps, selfPackageName = "com.salar.focuslock")

        assertEquals(1, result.size)
    }

    @Test
    fun `sorts results by app name case-insensitively`() {
        val apps = listOf(
            app("com.example.zebra", "zebra app"),
            app("com.example.apple", "Apple App"),
            app("com.example.mango", "mango App")
        )

        val result = AppDiscoveryProcessor.process(apps, selfPackageName = "com.salar.focuslock")

        assertEquals(listOf("Apple App", "mango App", "zebra app"), result.map { it.appName })
    }

    @Test
    fun `empty input yields empty output without throwing`() {
        val result = AppDiscoveryProcessor.process(emptyList(), selfPackageName = "com.salar.focuslock")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `system apps are passed through untouched, not filtered out`() {
        val apps = listOf(app("com.android.settings", "Settings", isSystem = true))
        val result = AppDiscoveryProcessor.process(apps, selfPackageName = "com.salar.focuslock")

        assertEquals(1, result.size)
        assertTrue(result.single().isSystemApp)
    }

    @Test
    fun `does not exclude apps that merely contain the self package name as a substring`() {
        // Regression guard: exclusion must be an exact package-name match, not a "contains" check.
        val apps = listOf(
            app("com.salar.focuslock", "Salar Focus Lock"),
            app("com.salar.focuslock.helper", "Some Other Helper App")
        )

        val result = AppDiscoveryProcessor.process(apps, selfPackageName = "com.salar.focuslock")

        assertEquals(1, result.size)
        assertFalse(result.any { it.packageName == "com.salar.focuslock" })
        assertTrue(result.any { it.packageName == "com.salar.focuslock.helper" })
    }
}
