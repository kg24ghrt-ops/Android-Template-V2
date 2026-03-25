package com.moweapp.antonio.engine

import android.content.Context
import org.mozilla.geckoview.*

class BrowserEngine {

    companion object {
        @Volatile
        private var runtime: GeckoRuntime? = null

        fun getRuntime(context: Context): GeckoRuntime {
            return runtime ?: synchronized(this) {
                runtime ?: GeckoRuntime.create(context.applicationContext).also {
                    runtime = it
                }
            }
        }
    }

    private var session: GeckoSession? = null
    private var canGoBackState = false

    fun init(
        context: Context,
        geckoView: GeckoView,
        interceptor: RequestInterceptor,
        performance: PerformanceManager
    ) {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .userAgentOverride("Mozilla/5.0 (Android 16; Mobile; rv:149.0) Gecko/149.0 Firefox/149.0")
            .suspendMediaWhenInactive(true)
            .build()

        session = GeckoSession(settings).apply {
            navigationDelegate = interceptor
            progressDelegate = performance
            open(getRuntime(context))
        }

        geckoView.setSession(session!!)
    }

    fun updateCanGoBack(value: Boolean) {
        canGoBackState = value
    }

    fun canGoBack(): Boolean = canGoBackState

    fun goBack() {
        session?.goBack()
    }

    fun loadUrl(url: String) {
        val fixed = if (!url.startsWith("http")) "https://$url" else url
        session?.loadUri(fixed)
    }
}