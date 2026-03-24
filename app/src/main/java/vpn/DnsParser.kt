package com.moweapp.antonio.vpn

import android.util.Log

/**
 * DnsParser
 *
 * Minimal, allocation-light DNS query parser.
 *
 * Only handles the question section of standard DNS queries (opcode 0).
 * We do NOT need a full RFC 1035 implementation — we just need the QNAME
 * so we can feed it to [DomainFilterEngine.isBlocked].
 *
 * DNS packet layout (bytes):
 *  0-1   Transaction ID
 *  2-3   Flags
 *  4-5   QDCOUNT (number of questions)
 *  6-7   ANCOUNT
 *  8-9   NSCOUNT
 *  10-11 ARCOUNT
 *  12+   Question section: <labels><QTYPE><QCLASS>
 *
 * QNAME encoding: sequence of [length][chars] pairs terminated by 0x00.
 */
object DnsParser {

    private const val TAG = "DnsParser"

    // DNS header is always 12 bytes
    private const val HEADER_SIZE = 12

    /**
     * Parse a DNS query and return the QNAME (domain name), or null on error.
     *
     * @param packet  Raw bytes (may be a UDP payload or a full IP packet
     *                depending on where in the pipeline this is called).
     * @param offset  Byte offset where the DNS header starts.
     * @param length  Number of valid bytes in [packet] from [offset].
     */
    fun extractDomain(packet: ByteArray, offset: Int = 0, length: Int = packet.size): String? {
        // Need at least the 12-byte header plus 1 label byte
        if (length < HEADER_SIZE + 1) return null

        return try {
            val flags = ((packet[offset + 2].toInt() and 0xFF) shl 8) or
                         (packet[offset + 3].toInt() and 0xFF)

            // QR bit (bit 15) must be 0 for a query
            val isQuery = (flags and 0x8000) == 0
            if (!isQuery) return null

            // QDCOUNT must be ≥ 1
            val qdCount = ((packet[offset + 4].toInt() and 0xFF) shl 8) or
                           (packet[offset + 5].toInt() and 0xFF)
            if (qdCount < 1) return null

            // Parse QNAME starting at byte 12
            parseQName(packet, offset + HEADER_SIZE, offset + length)
        } catch (e: Exception) {
            Log.w(TAG, "DNS parse error: ${e.message}")
            null
        }
    }

    /**
     * Decode the label-encoded QNAME into a dotted domain string.
     * e.g. [3]ads[7]example[3]com[0] → "ads.example.com"
     */
    private fun parseQName(packet: ByteArray, start: Int, end: Int): String? {
        val sb = StringBuilder()
        var pos = start

        while (pos < end) {
            val len = packet[pos].toInt() and 0xFF
            if (len == 0) break               // root label — end of QNAME

            // Compression pointer (top 2 bits = 11) — not expected in queries
            // but guard against it to avoid infinite loops
            if (len and 0xC0 == 0xC0) {
                Log.w(TAG, "Compression pointer in query — skipping")
                return null
            }

            pos++
            if (pos + len > end) return null  // truncated packet

            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(packet, pos, len, Charsets.US_ASCII).lowercase())
            pos += len
        }

        return if (sb.isEmpty()) null else sb.toString()
    }

    // ── IP/UDP unwrapping helpers ─────────────────────────────────────────────

    /**
     * Given a raw IPv4 packet, return the offset and length of the UDP payload
     * if this is a UDP/DNS packet (dest port 53), or null otherwise.
     *
     * Returns Pair(dnsOffset, dnsLength).
     */
    fun findDnsPayload(ipPacket: ByteArray, ipLength: Int): Pair<Int, Int>? {
        if (ipLength < 20) return null

        val protocol = ipPacket[9].toInt() and 0xFF
        if (protocol != 17 /* UDP */) return null

        // IP header length from lower nibble of first byte (in 32-bit words)
        val ipHeaderLen = (ipPacket[0].toInt() and 0x0F) * 4
        if (ipLength < ipHeaderLen + 8) return null

        // UDP destination port at offset ipHeaderLen + 2
        val destPort = ((ipPacket[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                        (ipPacket[ipHeaderLen + 3].toInt() and 0xFF)
        if (destPort != 53) return null

        // UDP payload starts at ipHeaderLen + 8
        val udpPayloadOffset = ipHeaderLen + 8
        val udpLength = ((ipPacket[ipHeaderLen + 4].toInt() and 0xFF) shl 8) or
                         (ipPacket[ipHeaderLen + 5].toInt() and 0xFF)
        val dnsLength = udpLength - 8   // subtract UDP header

        return if (dnsLength > 0 && udpPayloadOffset + dnsLength <= ipLength)
            Pair(udpPayloadOffset, dnsLength)
        else null
    }
}
