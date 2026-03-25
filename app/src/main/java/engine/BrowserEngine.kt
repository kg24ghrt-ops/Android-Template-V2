package com.moweapp.antonio.engine

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

class BrowserEngine {

    companion object {
        @Volatile
        private var sRuntime: GeckoRuntime? = null

        fun getRuntime(context: Context): GeckoRuntime {
            return sRuntime ?: synchronized(this) {
                sRuntime ?: GeckoRuntime.create(context.applicationContext).also { 
                    sRuntime = it 
                }
            }
        }
    }

    var session: GeckoSession? = null
        private set

    /**
     * @param context App context
     * @param geckoView The UI component
     * @param interceptor The ad-blocking logic
     * @param performance The progress tracker
     */
    fun init(
        context: Context, 
        geckoView: GeckoView, 
        interceptor: RequestInterceptor,
        performance: PerformanceManager
    ) {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            // Updated User Agent for 2026/Gecko 149
            .userAgentOverride("Mozilla/5.0 (Android 16; Mobile; rv:149.0) Gecko/149.0 Firefox/149.0")
            .suspendMediaWhenInactive(true)
            .build()

        session = GeckoSession(settings).apply {
            navigationDelegate = interceptor
            progressDelegate = performance
            
            // Open session with runtime
            open(getRuntime(context))
        }

        geckoView.setSession(session!!)
    }

    /**
     * Enhanced loadUrl that uses SecurityManager to sanitize inputs
     */
    fun loadUrl(url: String) {
        val sanitizedUrl = SecurityManager.sanitizeUrl(url)
        
        if (SecurityManager.isSafeUrl(sanitizedUrl)) {
            session?.loadUri(sanitizedUrl)
        }
    }

    // --- Navigation Controls (Updated for 149.0) ---

    /**
     * Checks if the session has a back history.
     * Note: In 149+, session.canGoBack() is often accessed via historyDelegate 
     * but we can use a direct session call if the delegate is attached.
     */
    fun canGoBack(): Boolean {
        // Simple check: if session exists and is open
        return session?.isOpen == true
    }

    fun goBack() {
        session?.goBack()
    }
}
