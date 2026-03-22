package com.moweapp.antonio.webview

import android.webkit.WebView
import androidx.compose.runtime.*

/**
 * State holder to bridge the gap between WebView logic and Compose UI.
 */
@Stable
class WebViewState {
    // Tracks the 0.0 to 1.0 progress for the LinearProgressIndicator
    var progress by mutableFloatStateOf(0f)
    
    // Logic: If progress is less than 1.0, we are still loading
    var isLoading by mutableStateOf(false)
    
    // Navigation states for the Back/Forward buttons in TopAppBar
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    
    // Direct reference used by IconButton(onClick = { state.webView?.goBack() })
    var webView: WebView? by mutableStateOf(null)
}
