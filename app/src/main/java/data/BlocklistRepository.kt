package com.moweapp.antonio.data

import android.content.Context
import android.util.Log

/**
 * BlocklistRepository
 *
 * Single source of truth for domain blocklists.
 *
 * Loading strategy (in priority order):
 *  1. "blocklist.txt" from the app's assets folder (if present).
 *  2. Built-in hardcoded list (always available as fallback).
 *
 * Format accepted:
 *  - Plain hostnames:       "example.com"
 *  - Hosts-file format:     "0.0.0.0 example.com"  or  "127.0.0.1 example.com"
 *  - Lines starting with #  → treated as comments, ignored
 *
 * To ship your own list, place a file named "blocklist.txt" in
 * src/main/assets/ and add one domain per line.
 *
 * This class has no instance state — all functions are suspend-friendly
 * and safe to call from any coroutine context.
 */
object BlocklistRepository {

    private const val TAG = "BlocklistRepository"
    private const val ASSET_FILE = "blocklist.txt"

    /**
     * Load all blocked domains and return them as a raw list of strings.
     * The [DomainFilterEngine] handles normalization.
     */
    fun loadDomains(context: Context): List<String> {
        val fromAsset = loadFromAssets(context)
        val builtin   = builtinList()

        val combined = (fromAsset + builtin).distinct()
        Log.i(TAG, "Loaded ${combined.size} domain entries " +
              "(${fromAsset.size} from asset, ${builtin.size} builtin)")
        return combined
    }

    // ── Asset loader ──────────────────────────────────────────────────────────

    private fun loadFromAssets(context: Context): List<String> {
        return try {
            context.assets.open(ASSET_FILE).bufferedReader().use { reader ->
                reader.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
            }
        } catch (e: Exception) {
            // Asset file is optional — absence is not an error
            Log.d(TAG, "No asset blocklist found ($ASSET_FILE) — using builtin only")
            emptyList()
        }
    }

    // ── Built-in list ─────────────────────────────────────────────────────────

    /**
     * A curated starter blocklist covering the most common ad/tracker networks.
     * Extend or replace this with a larger list (e.g. Steven Black's hosts).
     */
    private fun builtinList(): List<String> = listOf(
        // ── Google Ads ──
        "googleadservices.com",
        "googlesyndication.com",
        "googletagmanager.com",
        "googletagservices.com",
        "google-analytics.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",

        // ── DoubleClick ──
        "doubleclick.net",
        "ad.doubleclick.net",
        "stats.g.doubleclick.net",

        // ── Facebook / Meta ──
        "graph.facebook.com",
        "an.facebook.com",
        "connect.facebook.net",
        "pixel.facebook.com",

        // ── Amazon Ads ──
        "aax.amazon-adsystem.com",
        "fls-na.amazon.com",

        // ── General trackers ──
        "scorecardresearch.com",
        "quantserve.com",
        "adnxs.com",
        "adsrvr.org",
        "rubiconproject.com",
        "openx.net",
        "pubmatic.com",
        "casalemedia.com",
        "krxd.net",
        "smartadserver.com",
        "taboola.com",
        "outbrain.com",
        "revcontent.com",
        "criteo.com",
        "criteo.net",
        "moatads.com",
        "adsafeprotected.com",
        "doubleverify.com",
        "media.net",
        "advertising.com",
        "aolcdn.com",
        "mopub.com",
        "applovin.com",
        "vungle.com",
        "inmobi.com",
        "chartboost.com",
        "unityads.unity3d.com",
        "ads.yandex.ru",
        "pagead.l.google.com",
        "ads.twitter.com",
        "analytics.twitter.com",
        "static.ads-twitter.com",

        // ── Analytics & telemetry ──
        "mixpanel.com",
        "amplitude.com",
        "segment.io",
        "segment.com",
        "heapanalytics.com",
        "hotjar.com",
        "fullstory.com",
        "mouseflow.com",
        "loggly.com",
        "sentry.io",
        "bugsnag.com",

        // ── Spam / malware domains (common) ──
        "track.adform.net",
        "engine.4dsply.com",
        "ads.yieldmo.com",
        "sync.intentiq.com",
        "px.moatads.com",
        "ads.buzzoola.com",
        "rtb.gumgum.com",
        "servedby.flashtalking.com"
    )
}
