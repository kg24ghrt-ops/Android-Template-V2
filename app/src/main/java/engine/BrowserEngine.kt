package com.moweapp.antonio.engine

import android.content.Context
import org.mozilla.geckoview.*

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

    fun init(
        context: Context,
        geckoView: GeckoView,
        interceptor: RequestInterceptor,
        progress: GeckoSession.ProgressDelegate
    ) {
        // 🔥 Prevent duplicate session
        session?.close()

        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            // ✅ Use realistic UA (better compatibility)
            .userAgentOverride(
                "Mozilla/5.0 (Android 10; Mobile; rv:147.0) Gecko/147.0 Firefox/147.0"
            )
            .suspendMediaWhenInactive(true)
            .build()

        val newSession = GeckoSession(settings).apply {
            navigationDelegate = interceptor
            progressDelegate = progress
            open(getRuntime(context))
        }

        session = newSession

        // ✅ Safe binding
        geckoView.setSession(newSession)
    }

    fun loadUrl(url: String) {
        val sanitizedUrl = SecurityManager.sanitizeUrl(url)

        if (SecurityManager.isSafeUrl(sanitizedUrl)) {
            session?.loadUri(sanitizedUrl)
        }
    }

    // 🔥 Correct GeckoView navigation logic
    fun canGoBack(): Boolean {
        return session?.canGoBack ?: false
    }

    fun goBack() {
        if (canGoBack()) {
            session?.goBack()
        }
    }

    fun reload() {
        session?.reload()
    }

    fun destroy() {
        session?.close()
        session = null
    }
}