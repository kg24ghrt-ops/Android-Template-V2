package com.moweapp.antonio.webview

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SecureWebView(
    url: String,
    state: WebViewState, // Added this to sync with your UI
    modifier: Modifier = Modifier
) {
    val controller = WebViewController()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // Attach our controller logic
                controller.configureSettings(settings)
                
                // Sync the WebView instance back to the state
                state.webView = this
                
                // Custom WebViewClient to track loading progress
                webViewClient = controller.createWebViewClient(state)
                
                // Basic ChromeClient for video/progress support
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        state.progress = newProgress / 100f
                    }
                }

                loadUrl(url)
            }
        },
        update = { webView ->
            // Keep the state reference fresh
            state.webView = webView
        }
    )
}
