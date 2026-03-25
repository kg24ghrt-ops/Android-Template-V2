package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoResult
import com.moweapp.antonio.vpn.DomainFilterEngine

class RequestInterceptor(
    private val filterEngine: DomainFilterEngine,
    private val onUrlChanged: (String) -> Unit,
    private val onCanGoBackChanged: (Boolean) -> Unit
) : GeckoSession.NavigationDelegate {

    override fun onLoadRequest(
        session: GeckoSession,
        request: GeckoSession.NavigationDelegate.LoadRequest
    ): GeckoResult<GeckoSession.NavigationDelegate.AllowOrDeny> {

        return GeckoResult.fromValue(
            if (filterEngine.isBlocked(request.uri))
                GeckoSession.NavigationDelegate.AllowOrDeny.DENY
            else
                GeckoSession.NavigationDelegate.AllowOrDeny.ALLOW
        )
    }

    // ⚠️ STILL SUPPORTED BUT NOT FUTURE SAFE
    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
        hasUserGesture: Boolean
    ) {
        url?.let { onUrlChanged(it) }
    }

    // ✅ REQUIRED for back navigation
    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
        onCanGoBackChanged(canGoBack)
    }
}