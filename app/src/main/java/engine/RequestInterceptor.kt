package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import com.moweapp.antonio.vpn.DomainFilterEngine

class RequestInterceptor(
    private val filterEngine: DomainFilterEngine
) : NavigationDelegate {

    override fun onLoadRequest(
        session: GeckoSession,
        request: NavigationDelegate.LoadRequest
    ): GeckoResult<AllowOrDeny> {

        val url = request.uri
        val shouldBlock = filterEngine.isBlocked(url)

        return GeckoResult.fromValue(
            if (shouldBlock) AllowOrDeny.DENY
            else AllowOrDeny.ALLOW
        )
    }
}