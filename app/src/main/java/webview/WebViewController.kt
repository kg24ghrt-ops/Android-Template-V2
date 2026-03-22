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
            // 🔥 THE RESET FIX: Modern sites require these to load data
            domStorageEnabled = true
            databaseEnabled = true
            
            // 🔥 THE RESET FIX: Pretend to be a Pixel 7 / Chrome browser
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
            
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            allowFileAccess = true
            allowContentAccess = true
        }
    }

    fun createWebViewClient(state: WebViewState): WebViewClient = object : WebViewClient() {
        
        // 🔥 POWER PLANT AD-BLOCKER: Intercepts and kills ads before they load
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url.toString()
            val adKeywords = listOf("doubleclick", "googleadservices", "popads", "adskeeper", "exoclick", "adsterra")
            
            if (adKeywords.any { url.contains(it) }) {
                // Return an empty response to "silence" the ad
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

        // Keep the user inside the app
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false 
        }
    }

    fun setupDownloadListener(webView: WebView) {
        // Placeholder for future download handling
    }
}
