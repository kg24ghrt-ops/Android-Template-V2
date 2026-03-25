package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import com.moweapp.antonio.vpn.DomainFilterEngine

class RequestInterceptor(
    private val filterEngine: DomainFilterEngine,
    private val onUrlChanged: (String) -> Unit
) : NavigationDelegate {

    override fun onLoadRequest(session: GeckoSession, request: NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
        val url = request.uri
        
        // Matches your DomainFilterEngine.isBlocked(domain: String)
        val shouldBlock = filterEngine.isBlocked(url)

        return if (shouldBlock) {
            GeckoResult.fromValue(AllowOrDeny.DENY)
        } else {
            GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }
    }

    // 🔥 UPDATED SIGNATURE FOR GECKO 149
    // Note the explicit use of the ContentPermission type in the List
    override fun onLocationChange(
        session: GeckoSession, 
        url: String?, 
        permits: List<org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission>
    ) {
        url?.let { onUrlChanged(it) }
    }
}
