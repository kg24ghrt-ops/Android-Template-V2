package com.moweapp.antonio.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.annotation.WorkerThread

/**
 * Manages WebView configuration, security policies, and ad-blocking interception.
 */
class WebViewController(
    private val state: WebViewState, // Required constructor parameter
    private val allowedDomain: String = "vidbox.cc"
) {

    // Dummy AdBlocker if you haven't created the class yet to prevent compilation errors
    // Replace with your actual AdBlocker class if it exists
    private val adBlocker = object {
        fun isAd(url: String) = false
        fun createEmptyResource(): WebResourceResponse? = null
        fun getAdCleanupScript() = ""
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureSettings(settings: WebSettings) {
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // FIXED: Enable autoplay for video sites
            mediaPlaybackRequiresUserAction = false
            
            // Security settings
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }
    }

    // ADDED: Missing method referenced in your SecureWebView
    fun setupDownloadListener(webView: WebView) {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            // Logic for handling downloads can go here (e.g., Opening a Browser or Intent)
        }
    }

    fun createWebChromeClient(): WebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            state.progress = newProgress / 100f
            state.isLoading = newProgress < 100
        }
    }

    fun createWebViewClient(): WebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            state.isLoading = true
            state.canGoBack = view?.canGoBack() ?: false
            state.canGoForward = view?.canGoForward() ?: false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url ?: "")
            state.isLoading = false
            state.canGoBack = view?.canGoBack() ?: false
            state.canGoForward = view?.canGoForward() ?: false
            
            view?.evaluateJavascript(adBlocker.getAdCleanupScript(), null)
        }
    }
}
