package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.AllowOrDeny
import com.moweapp.antonio.vpn.DomainFilterEngine

class RequestInterceptor(
    private val filterEngine: DomainFilterEngine,
    private val onUrlChanged: (String) -> Unit,
    private val onCanGoBackChanged: (Boolean) -> Unit
) : GeckoSession.NavigationDelegate {

    override fun onLoadRequest(
        session: GeckoSession,
        request: GeckoSession.NavigationDelegate.LoadRequest
    ): GeckoResult<AllowOrDeny> {

        val shouldBlock = filterEngine.isBlocked(request.uri)

        return GeckoResult.fromValue(
            if (shouldBlock) AllowOrDeny.DENY
            else AllowOrDeny.ALLOW
        )
    }

    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
        hasUserGesture: Boolean
    ) {
        url?.let { onUrlChanged(it) }
    }

    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
        onCanGoBackChanged(canGoBack)
    }
}