package com.moweapp.antonio.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import java.io.ByteArrayInputStream

class WebViewController(
    private val state: WebViewState,
    private val allowedDomain: String = "vidbox.cc"
) {
    @SuppressLint("SetJavaScriptEnabled")
    fun configureSettings(settings: WebSettings) {
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            
            // 🛡️ SECURITY FIX: Disable these to prevent "File Uri" crashes on modern Android
            allowFileAccess = false
            allowContentAccess = false
            
            // 🛡️ STABILITY FIX: Use a stable Mobile User Agent instead of Desktop
            // Some mobile GPUs crash trying to render the Desktop version of complex players
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
            
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // Critical for streaming sites
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        
        // Ensure cookies are ready before the first load
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(state.webView, true)
    }

    fun createWebViewClient(state: WebViewState): WebViewClient = object : WebViewClient() {
        
        // 🛑 CRASH FIX: Check for Null requests before processing
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url?.toString() ?: return null
            
            // AD-BLOCKER: Focus on the high-frequency "reset" triggers
            val adKeywords = listOf("doubleclick", "googleadservices", "popads", "adskeeper", "exoclick")
            if (adKeywords.any { url.contains(it, ignoreCase = true) }) {
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
            }
            return super.shouldInterceptRequest(view, request)
        }

        // 🛑 LOOP FIX: Remove the "auto-reload" on error. 
        // If the site is down, auto-reload causes an infinite loop that kills the app process.
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            if (request?.isForMainFrame == true) {
                state.isLoading = false
                // Instead of reloading, we just stop. This prevents the "Automatic Closing" crash.
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            state.isLoading = true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            state.isLoading = false
            CookieManager.getInstance().flush()
        }

        // Prevent external app "Leaps" (e.g., opening Play Store)
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            return if (url.contains(allowedDomain)) {
                false // Stay in WebView
            } else {
                true // Block the redirect to stop the app from closing/switching
            }
        }
    }
}
