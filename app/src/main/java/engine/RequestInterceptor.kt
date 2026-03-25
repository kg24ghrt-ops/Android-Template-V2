package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.NavigationDelegate.LoadRequest
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import com.moweapp.antonio.vpn.DomainFilterEngine

class RequestInterceptor(
    private val filterEngine: DomainFilterEngine,
    private val onUrlChanged: (String) -> Unit
) : NavigationDelegate {

    // 1. Fix: Use the exact 'AllowOrDeny' type and correct override signature
    override fun onLoadRequest(session: GeckoSession, request: LoadRequest): GeckoResult<AllowOrDeny>? {
        val url = request.uri
        
        // 2. Fix: Check if your filter engine uses 'isBlocked' or 'isDomainBlocked'
        // If your DomainFilterEngine uses a different name, adjust it here.
        val shouldBlock = filterEngine.isDomainBlocked(url)

        return if (shouldBlock) {
            GeckoResult.fromValue(AllowOrDeny.DENY)
        } else {
            GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }
    }

    // 3. Fix: Modern GeckoView signature for onLocationChange
    override fun onLocationChange(session: GeckoSession, url: String?) {
        url?.let { onUrlChanged(it) }
    }
}
