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
    state: WebViewState, // Ensure this parameter exists
    modifier: Modifier = Modifier
) {
    // Pass the state to the controller
    val controller = WebViewController(state)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                controller.configureSettings(settings)
                state.webView = this
                
                // FIXED: Passing state to the client
                webViewClient = controller.createWebViewClient(state)
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        state.progress = newProgress / 100f
                    }
                }

                loadUrl(url)
            }
        },
        update = { webView ->
            state.webView = webView
        }
    )
}
