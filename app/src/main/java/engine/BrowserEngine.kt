package com.moweapp.antonio.engine

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

/**
 * Refactored BrowserEngine (Kotlin).
 * Now integrates SecurityManager and RequestInterceptor for the full Antonio experience.
 */
class BrowserEngine {

    companion object {
        @Volatile
        private var sRuntime: GeckoRuntime? = null

        fun getRuntime(context: Context): GeckoRuntime {
            return sRuntime ?: synchronized(this) {
                sRuntime ?: GeckoRuntime.create(context.getApplicationContext()).also { 
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
     * @param interceptor The ad-blocking logic we built in Step 3
     */
    fun init(
    context: Context, 
    geckoView: GeckoView, 
    interceptor: RequestInterceptor,
    performance: PerformanceManager // 🔥 NEW
) {
    val settings = GeckoSessionSettings.Builder()
        .usePrivateMode(false)
        .userAgentOverride("Mozilla/5.0 (Android 13; Mobile; rv:131.0) Gecko/131.0 Firefox/131.0")
        .suspendMediaWhenInactive(true)
        .build()

    session = GeckoSession(settings).apply {
        navigationDelegate = interceptor
        progressDelegate = performance // 🔥 ATTACH PROGRESS TRACKER
        
        open(getRuntime(context))
    }

    geckoView.setSession(session!!)
    }


    /**
     * Enhanced loadUrl that uses SecurityManager to sanitize inputs
     */
    fun loadUrl(url: String) {
        // 1. Sanitize (add https or turn into a search query)
        val sanitizedUrl = SecurityManager.sanitizeUrl(url)
        
        // 2. Validate (Check for forbidden schemes like file://)
        if (SecurityManager.isSafeUrl(sanitizedUrl)) {
            session?.loadUri(sanitizedUrl)
        }
    }

    // --- Navigation Controls ---
    fun goBack() = session?.goBack()
    fun goForward() = session?.goForward()
    fun reload() = session?.reload()
    
    // Check if the session can navigate back (useful for the Android Back Button)
    fun canGoBack(): Boolean = session?.canGoBack() ?: false
}
