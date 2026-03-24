package com.moweapp.antonio.vpn

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

/**
 * DomainFilterEngine
 *
 * Handles blocked-domain lookups with:
 * - O(1) HashSet lookups
 * - Atomic reference swap for thread safety (no locks on hot path)
 * - Fast-path host extraction before expensive URI parsing
 * - Recursive subdomain matching
 * - Background coroutine loading
 *
 * Thread safety: [blockedDomains] is an AtomicReference to an immutable Set.
 * Null means "not loaded yet". Callers can check [isLoaded] before querying.
 */
class DomainFilterEngine {

    private val TAG = "DomainFilterEngine"

    /**
     * Holds the current blocked domain set.
     * Null = not yet loaded. Non-null = ready for queries.
     */
    private val blockedDomains = AtomicReference<Set<String>?>(null)

    /** True once the blocklist has been loaded at least once. */
    val isLoaded: Boolean get() = blockedDomains.get() != null

    // ── Loading ──────────────────────────────────────────────────────────────

    /**
     * Load [domains] into the engine on the IO dispatcher.
     * Atomically swaps the internal set so in-flight queries are never blocked.
     *
     * @param domains Raw domain strings (URLs, hostnames, or prefixed lines).
     */
    fun loadAsync(domains: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            val normalized = domains
                .mapNotNull { extractHost(it) }
                .toHashSet()

            blockedDomains.set(normalized)
            Log.d(TAG, "Loaded ${normalized.size} blocked domains")
        }
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns true if [domain] (or any parent domain) is blocked.
     *
     * Returns false if the engine has not finished loading yet — fail-open
     * so no DNS is blocked before the list is ready.
     */
    fun isBlocked(domain: String): Boolean {
        val set = blockedDomains.get() ?: return false   // not loaded → allow
        val host = extractHost(domain) ?: return false

        return matchesAny(host, set)
    }

    /**
     * Recursive subdomain check.
     * "ads.example.com" is blocked if "example.com" is in the set.
     */
    private fun matchesAny(host: String, set: Set<String>): Boolean {
        if (set.contains(host)) return true

        val dotIndex = host.indexOf('.')
        if (dotIndex == -1) return false                 // bare TLD — no parent
        return matchesAny(host.substring(dotIndex + 1), set)
    }

    // ── Host extraction ───────────────────────────────────────────────────────

    /**
     * Extract the hostname from a raw string.
     *
     * Fast-path: if the string contains no '/' and no ':', it is already
     * a plain hostname (e.g. "example.com") — skip URI allocation entirely.
     *
     * Falls back to URI parsing for full URLs such as
     * "https://ads.example.com/tracker?id=1".
     *
     * Returns null for malformed or empty inputs.
     */
    internal fun extractHost(raw: String): String? {
        val trimmed = raw.trim().lowercase()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null

        // Strip common blocklist prefixes like "0.0.0.0 example.com"
        val candidate = trimmed
            .removePrefix("0.0.0.0 ")
            .removePrefix("127.0.0.1 ")
            .trim()

        // ── Fast path ──────────────────────────────────────────────────────
        // A plain hostname has no slash and no colon (no scheme, no port).
        if (!candidate.contains('/') && !candidate.contains(':')) {
            return candidate.takeIf { it.isNotEmpty() }
        }

        // ── URI parse path ─────────────────────────────────────────────────
        return try {
            val uri = URI(
                if (candidate.contains("://")) candidate
                else "https://$candidate"           // ensure URI can parse host
            )
            uri.host?.lowercase()?.trimStart('.')
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse host from: $raw")
            null
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    /** Number of domains currently loaded. 0 if not yet ready. */
    fun blockedCount(): Int = blockedDomains.get()?.size ?: 0
}
