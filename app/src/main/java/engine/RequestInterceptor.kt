package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoResult
import com.moweapp.antonio.vpn.DomainFilterEngine

class RequestInterceptor(
    private val filterEngine: DomainFilterEngine,
    private val onUrlChanged: (String) -> Unit
) : NavigationDelegate {

    // Modern GeckoView uses GeckoResult<Int> for allowing/denying
    override fun onLoadRequest(session: GeckoSession, request: NavigationDelegate.LoadRequest): GeckoResult<Int>? {
        val url = request.uri
        
        return if (filterEngine.isDomainBlocked(url)) {
            // Constant for Deny (0 = Deny, 1 = Allow)
            GeckoResult.fromValue(NavigationDelegate.LoadRequest.DENY)
        } else {
            GeckoResult.fromValue(NavigationDelegate.LoadRequest.ALLOW)
        }
    }

    // Updated to match the new 149 API signature
    override fun onLocationChange(session: GeckoSession, url: String?, permits: List<GeckoSession.PermissionDelegate.ContentPermission>) {
        url?.let { onUrlChanged(it) }
    }
}
