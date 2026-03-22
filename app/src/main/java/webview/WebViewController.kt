package com.moweapp.antonio.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.annotation.WorkerThread

class WebViewController(
    private val state: WebViewState,
    private val allowedDomain: String = "vidbox.cc"
) {
    @SuppressLint("SetJavaScriptEnabled")
    fun configureSettings(settings: WebSettings) {
        with(settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // FIXED: This is the correct way to set media playback in modern Android
            mediaPlaybackRequiresUserAction = false
            
            allowFileAccess = false
            allowContentAccess = false
        }
    }

    // FIXED: Added 'state' as a parameter to match the call in SecureWebView
    fun createWebViewClient(state: WebViewState): WebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            state.isLoading = true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            state.isLoading = false
            state.canGoBack = view?.canGoBack() ?: false
            state.canGoForward = view?.canGoForward() ?: false
        }
    }

    fun setupDownloadListener(webView: WebView) {
        // Placeholder for download logic
    }
}
