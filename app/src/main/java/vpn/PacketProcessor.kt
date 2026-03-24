package com.moweapp.antonio.vpn

import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * PacketProcessor
 *
 * Core read/filter loop for the VPN tunnel.
 *
 * Architecture:
 *  - Reads raw IPv4 packets from the tun interface (via [vpnInput]).
 *  - For UDP port 53 (DNS) packets, extracts the queried domain name.
 *  - Blocked domains → packet is silently dropped (never written back).
 *  - Allowed domains → packet is written to [vpnOutput] for normal routing.
 *
 * This intentionally keeps packet processing minimal:
 *  - No deep packet inspection beyond DNS question extraction.
 *  - No TCP reassembly.
 *  - Non-DNS traffic passes through unconditionally.
 *
 * Run [start] on a background thread / coroutine. Call [stop] to shut down.
 */
class PacketProcessor(
    private val vpnInput: FileInputStream,
    private val vpnOutput: FileOutputStream,
    private val filterEngine: DomainFilterEngine,
    private val onDomainBlocked: (String) -> Unit = {}
) {

    private val TAG = "PacketProcessor"

    // Max IPv4 packet size
    private val BUFFER_SIZE = 65535

    @Volatile
    private var running = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Start the blocking read loop. This call blocks until [stop] is invoked.
     * Run this on a dedicated background thread (e.g. via [kotlinx.coroutines.Dispatchers.IO]).
     */
    fun start() {
        running = true
        Log.i(TAG, "PacketProcessor started")

        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        val rawBuffer = buffer.array()

        while (running) {
            try {
                // Read one IP packet from the tun interface.
                // read() blocks until data is available.
                val length = vpnInput.read(rawBuffer)
                if (length <= 0) continue

                processPacket(rawBuffer, length)

            } catch (e: Exception) {
                if (running) {
                    Log.e(TAG, "Packet read error: ${e.message}")
                }
                // If !running, the fd was closed intentionally — exit loop.
            }
        }

        Log.i(TAG, "PacketProcessor stopped")
    }

    /** Signal the read loop to exit on next iteration. */
    fun stop() {
        running = false
    }

    // ── Packet handling ───────────────────────────────────────────────────────

    /**
     * Inspect a single raw IP packet and decide whether to forward or drop it.
     */
    private fun processPacket(packet: ByteArray, length: Int) {
        // Try to extract a DNS domain from this packet.
        val dnsDomain = extractDnsQuery(packet, length)

        when {
            dnsDomain != null && filterEngine.isBlocked(dnsDomain) -> {
                // DROP: blocked domain — do not write to output.
                Log.d(TAG, "BLOCKED: $dnsDomain")
                onDomainBlocked(dnsDomain)
            }
            else -> {
                // FORWARD: non-DNS traffic, or allowed domain.
                forwardPacket(packet, length)
            }
        }
    }

    /**
     * Attempt to parse a DNS query from the packet.
     * Returns the queried domain, or null if this is not a DNS query packet.
     */
    private fun extractDnsQuery(packet: ByteArray, length: Int): String? {
        val (dnsOffset, dnsLength) = DnsParser.findDnsPayload(packet, length) ?: return null
        return DnsParser.extractDomain(packet, dnsOffset, dnsLength)
    }

    /**
     * Write [length] bytes of [packet] to the tun output stream,
     * returning the packet to the OS networking stack.
     */
    private fun forwardPacket(packet: ByteArray, length: Int) {
        try {
            vpnOutput.write(packet, 0, length)
        } catch (e: Exception) {
            if (running) Log.e(TAG, "Packet write error: ${e.message}")
        }
    }
}
