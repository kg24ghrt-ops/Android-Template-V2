package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession

class ProgressHandler(
    private val onUrlChanged: (String) -> Unit
) : GeckoSession.ProgressDelegate {

    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        flags: Int
    ) {
        url?.let { onUrlChanged(it) }
    }
}