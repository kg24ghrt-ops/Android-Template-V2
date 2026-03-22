package com.moweapp.antonio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moweapp.antonio.webview.SecureWebView
import com.moweapp.antonio.webview.WebViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // State management for the WebView
    val state = remember { WebViewState() }
    var isFullscreen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Hide TopBar in fullscreen mode for immersive video
            AnimatedVisibility(visible = !isFullscreen) {
                Column {
                    TopAppBar(
                        title = { Text("Antonio", style = MaterialTheme.typography.titleMedium) },
                        actions = {
                            IconButton(onClick = { state.webView?.goBack() }, enabled = state.canGoBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            IconButton(onClick = { state.webView?.goForward() }, enabled = state.canGoForward) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                            }
                            IconButton(onClick = { state.webView?.reload() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload")
                            }
                        }
                    )
                    // FIXED: LinearProgressIndicator in Material3 uses a direct Float for progress, not a lambda
                    if (state.isLoading) {
                        LinearProgressIndicator(
                            progress = state.progress, 
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        // In fullscreen, we ignore the Scaffold padding to take over the whole screen
        val finalPadding = if (isFullscreen) PaddingValues(0.dp) else padding
        
        Box(modifier = Modifier.padding(finalPadding)) {
            SecureWebView(
                url = "https://vidbox.cc/home",
                state = state,
                modifier = Modifier.fillMaxSize()
            )
            
            // Exit Fullscreen Button Overlay (only visible in fullscreen)
            // Note: If Icons.Default.FullscreenExit fails, ensure 'androidx.compose.material:material-icons-extended' is in build.gradle
            if (isFullscreen) {
                FilledIconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier.padding(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit Fullscreen")
                }
            }
        }
    }
}
