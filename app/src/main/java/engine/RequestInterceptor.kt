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
        
        // 🔥 SYNCED: Changed 'isDomainBlocked' to 'isBlocked' to match your Engine
        val shouldBlock = filterEngine.isBlocked(url)

        return if (shouldBlock) {
            GeckoResult.fromValue(AllowOrDeny.DENY)
        } else {
            GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }
    }

    // 🔥 SYNCED: Gecko 149 requires the 'permits' parameter
    override fun onLocationChange(
        session: GeckoSession, 
        url: String?, 
        permits: List<GeckoSession.PermissionDelegate.ContentPermission>
    ) {
        url?.let { onUrlChanged(it) }
    }
}
