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
            
            // FIXED: Changed 'UserAction' to 'UserGesture'
            // This is the correct property name for Android WebSettings
            mediaPlaybackRequiresUserGesture = false
            
            allowFileAccess = false
            allowContentAccess = false
        }
    }

    // Pass the 'state' here so SecureWebView can update the loading bar
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
        // Placeholder for future download handling
    }
}
