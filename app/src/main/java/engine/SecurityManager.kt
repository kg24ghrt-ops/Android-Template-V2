package com.moweapp.antonio.engine

import android.net.Uri
import java.util.Locale

/**
 * SecurityManager handles URL validation and protocol safety.
 * It prevents the browser from loading dangerous or restricted schemes.
 */
object SecurityManager {

    // Schemes that are explicitly forbidden for security reasons
    private val FORBIDDEN_SCHEMES = setOf(
        "file",       // Prevent local file system access
        "javascript", // Prevent XSS via address bar
        "data",       // Prevent data-URI phishing
        "content"     // Prevent Android ContentProvider exploits
    )

    /**
     * Checks if a URL is safe to load.
     * @param url The raw string from the user or a link.
     * @return true if the URL is safe, false otherwise.
     */
    fun isSafeUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return false
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return true // Assume relative/http if no scheme

        // Block forbidden schemes
        if (FORBIDDEN_SCHEMES.contains(scheme)) {
            return false
        }

        return true
    }

    /**
     * Ensures the URL has a valid protocol (defaulting to https).
     */
    fun sanitizeUrl(url: String): String {
        var trimmed = url.trim()
        
        // If it's a search query instead of a URL (no dots and no scheme)
        if (!trimmed.contains(".") && !trimmed.contains("://")) {
            return "https://www.google.com/search?q=$trimmed"
        }

        // Add default https if missing
        if (!trimmed.contains("://")) {
            trimmed = "https://$trimmed"
        }

        return trimmed
    }
}
