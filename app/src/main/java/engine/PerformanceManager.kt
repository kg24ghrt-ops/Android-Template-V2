package com.moweapp.antonio.engine

import org.mozilla.geckoview.GeckoSession

class PerformanceManager(
    private val onProgressUpdate: (Int) -> Unit,
    private val onLoadingStateChanged: (Boolean) -> Unit,
    private val onUrlChanged: (String) -> Unit,
    private val onVoice: (String) -> Unit
) : GeckoSession.ProgressDelegate {

    private var lastSpokenProgress = 0
    private var lastVoiceTime = 0L

    // 🔥 PAGE START (always reliable)
    override fun onPageStart(session: GeckoSession, url: String) {
        onLoadingStateChanged(true)
        onUrlChanged(url)

        lastSpokenProgress = 0
        speak("Loading started")
    }

    // 🔥 PAGE END (always reliable)
    override fun onPageStop(session: GeckoSession, success: Boolean) {
        onLoadingStateChanged(false)

        if (success) {
            onProgressUpdate(100)
            speak("Page loaded")
        } else {
            speak("Failed to load page")
        }
    }

    // 🔥 PROGRESS UPDATES (high frequency → must throttle)
    override fun onProgressChange(session: GeckoSession, progress: Int) {
        onProgressUpdate(progress)

        // Only speak every 20% AND with time debounce
        if (progress - lastSpokenProgress >= 20 && shouldSpeak()) {
            lastSpokenProgress = progress
            speak(getVoiceMessage(progress))
        }
    }

    // ─────────────────────────────────────────────
    // 🧠 SMART VOICE SYSTEM
    // ─────────────────────────────────────────────

    private fun speak(message: String) {
        onVoice(message)
    }

    private fun shouldSpeak(): Boolean {
        val now = System.currentTimeMillis()
        return if (now - lastVoiceTime > 1500) {
            lastVoiceTime = now
            true
        } else {
            false
        }
    }

    private fun getVoiceMessage(progress: Int): String {
        return when {
            progress < 10 -> "Starting page"
            progress < 40 -> "Loading content"
            progress < 70 -> "Still loading"
            progress < 95 -> "Almost there"
            else -> "Finishing"
        }
    }
}