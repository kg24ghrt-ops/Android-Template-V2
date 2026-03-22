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
            // 1. 🔥 THE RESET FIX: Enable the "Brain"
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            
            // 2. 🔥 THE IDENTITY FIX: Use a Desktop identity (most stable)
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
            
            // 3. 🔥 THE FIREWALL FIX: Allow mixed content (Vidbox uses multiple servers)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // 4. Performance & Video Stability
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            
            // 5. Security bypass for streamers
            allowFileAccess = true
            allowContentAccess = true
        }

        // 6. 🔥 THE COOKIE FIX: Sync cookies properly to avoid "Reset"
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(state.webView, true)
    }

    fun createWebViewClient(state: WebViewState): WebViewClient = object : WebViewClient() {
        
        // 🔥 POWER PLANT AD-BLOCKER
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url.toString()
            // Expanded list to kill Vidbox's common pop-up triggers
            val adKeywords = listOf(
                "doubleclick", "googleadservices", "popads", "adskeeper", 
                "exoclick", "adsterra", "bet365", "onclickads", "vidoomy"
            )
            
            if (adKeywords.any { url.contains(it, ignoreCase = true) }) {
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            state.isLoading = true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            state.isLoading = false
            state.canGoBack = view?.canGoBack() ?: false
            state.canGoForward = view?.canGoForward() ?: false
        }

        // Keep all navigation inside the app box
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url.toString()
            return if (url.contains(allowedDomain)) {
                false // Load in WebView
            } else {
                false // Still load in WebView (blocks pop-outs)
            }
        }

        // 🔥 THE ERROR CATCHER: Let's see if we can get more info on the reset
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            // If it resets, try to reload once automatically
            if (error?.errorCode == ERROR_CONNECT || error?.errorCode == ERROR_TIMEOUT) {
                view?.reload()
            }
        }
    }

    fun setupDownloadListener(webView: WebView) {
        // Placeholder
    }
}
