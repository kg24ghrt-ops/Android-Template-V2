package com.moweapp.antonio.browser

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/**
 * BrowserLauncher
 *
 * Opens URLs using Chrome Custom Tabs.
 *
 * Because the app routes all traffic through [MyVpnService], any URL opened
 * here automatically benefits from DNS-level ad blocking — no extra
 * configuration is required.
 *
 * Dependency (add to build.gradle.kts :app):
 *   implementation("androidx.browser:browser:1.8.0")
 *
 * Usage:
 *   BrowserLauncher.open(context, "https://vidbox.cc/home")
 */
object BrowserLauncher {

    private const val TAG = "BrowserLauncher"

    /** Default target URL — the streaming site to open. */
    const val DEFAULT_URL = "https://vidbox.cc/home"

    /**
     * Open [url] in a Chrome Custom Tab.
     *
     * The Custom Tab shares the user's existing Chrome session (cookies,
     * logins) and renders inside the host app with a branded toolbar.
     *
     * If Chrome is not installed, Android falls back to the default browser
     * or the URL picker — no crash.
     *
     * @param context   Activity or application context.
     * @param url       Full URL to open (must include scheme).
     * @param toolbarColor  Hex color string for the Custom Tab toolbar, e.g. "#1A1A2E".
     */
    fun open(
        context: Context,
        url: String = DEFAULT_URL,
        toolbarColor: String = "#0F3460"
    ) {
        if (url.isBlank()) {
            Log.w(TAG, "open() called with blank URL — ignoring")
            return
        }

        val parsedUri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URL: $url — ${e.message}")
            return
        }

        val color = try {
            Color.parseColor(toolbarColor)
        } catch (e: Exception) {
            Log.w(TAG, "Invalid toolbarColor '$toolbarColor', using default")
            Color.parseColor("#0F3460")
        }

        val colorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(color)
            .setSecondaryToolbarColor(color)
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(colorSchemeParams)
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)        // hides toolbar on scroll
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .setInstantAppsEnabled(false)
            .build()

        // Ensure we don't accidentally open the Custom Tab with the app's
        // own task stack — use FLAG_ACTIVITY_NEW_TASK when launched from a
        // non-Activity context.
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            customTabsIntent.launchUrl(context, parsedUri)
            Log.i(TAG, "Launched URL: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Custom Tab: ${e.message}")
            // Fallback: plain intent
            fallbackOpen(context, parsedUri)
        }
    }

    /** Last-resort fallback: open with a plain VIEW intent. */
    private fun fallbackOpen(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback open also failed: ${e.message}")
        }
    }
}
