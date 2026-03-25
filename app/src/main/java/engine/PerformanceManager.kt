package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ProgressDelegate

/**
 * PerformanceManager tracks page load progress and security states.
 * It provides callbacks to update the UI progress bar.
 */
class PerformanceManager(
    private val onProgressUpdate: (Int) -> Unit,
    private val onLoadingStateChanged: (Boolean) -> Unit
) : ProgressDelegate {

    override fun onProgressChange(session: GeckoSession, progress: Int) {
        // progress is 0-100
        onProgressUpdate(progress)
        
        // Hide progress bar when it reaches 100
        if (progress >= 100) {
            onLoadingStateChanged(false)
        } else {
            onLoadingStateChanged(true)
        }
    }

    override fun onSecurityChange(
        session: GeckoSession,
        securityInfo: ProgressDelegate.SecurityInformation
    ) {
        // Optional: Use this to show a "Lock" icon for HTTPS
    }
}
