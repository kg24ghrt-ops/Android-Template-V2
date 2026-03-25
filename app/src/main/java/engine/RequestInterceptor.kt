package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoResult
import com.moweapp.antonio.vpn.DomainFilterEngine

class RequestInterceptor(
    private val filterEngine: DomainFilterEngine,
    private val onUrlChanged: (String) -> Unit
) : NavigationDelegate {

    override fun onLoadRequest(
        session: GeckoSession,
        request: NavigationDelegate.LoadRequest
    ): GeckoResult<NavigationDelegate.Result> {

        val url = request.uri
        val shouldBlock = filterEngine.isBlocked(url)

        return GeckoResult.fromValue(
            if (shouldBlock) {
                NavigationDelegate.Result.DENY
            } else {
                NavigationDelegate.Result.ALLOW
            }
        )
    }

    override fun onLocationChange(
        session: GeckoSession,
        url: String?
    ) {
        url?.let { onUrlChanged(it) }
    }
}