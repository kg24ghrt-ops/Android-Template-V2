package com.moweapp.antonio.engine

import android.net.Uri
import com.moweapp.antonio.vpn.DomainFilterEngine
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate

/**
 * The RequestInterceptor acts as the "Bouncer."
 * It intercepts every request and checks it against your DomainFilterEngine.
 */
class RequestInterceptor(
    private val filterEngine: DomainFilterEngine,
    private val onUrlChanged: (String) -> Unit // Callback to update the UI URL bar
) : NavigationDelegate {

    /**
     * Triggered every time the browser wants to load a new resource or page.
     */
    override fun onLoadRequest(session: GeckoSession, request: NavigationDelegate.LoadRequest): GeckoResult<NavigationDelegate.AllowOrDeny>? {
        val uri = Uri.parse(request.uri)
        val host = uri.host

        // 🛡️ THE AD-BLOCK CHECK
        // We call your existing Kotlin DomainFilterEngine logic here
        if (host != null && filterEngine.isBlocked(host)) {
            // SILENTLY BLOCK: The request never leaves the device
            return GeckoResult.fromValue(NavigationDelegate.AllowOrDeny.DENY)
        }

        // ALLOW: Safe domain
        return GeckoResult.fromValue(NavigationDelegate.AllowOrDeny.ALLOW)
    }

    /**
     * Triggered when the main URL in the address bar changes (e.g., following a link).
     */
    override fun onLocationChange(session: GeckoSession, url: String?) {
        super.onLocationChange(session, url)
        url?.let { onUrlChanged(it) }
    }
}
