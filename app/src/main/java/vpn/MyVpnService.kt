package com.moweapp.antonio.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.moweapp.antonio.data.BlocklistRepository
import com.moweapp.antonio.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * MyVpnService
 *
 * Core VPN service that:
 *  1. Creates a local TUN interface (no external server needed).
 *  2. Feeds all traffic through [PacketProcessor] for DNS-level ad blocking.
 *  3. Broadcasts blocked-count updates to [MainActivity] via LocalBroadcast.
 *
 * Lifecycle:
 *  START_VPN action → builds TUN → starts [PacketProcessor] on IO thread.
 *  STOP_VPN action  → tears down TUN → stops processor.
 *
 * Manifest requirements (add to AndroidManifest.xml):
 *  <service android:name=".vpn.MyVpnService"
 *           android:permission="android.permission.BIND_VPN_SERVICE"
 *           android:exported="false">
 *      <intent-filter>
 *          <action android:name="android.net.VpnService"/>
 *      </intent-filter>
 *  </service>
 *
 *  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
 *  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
 */
class MyVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.moweapp.antonio.START_VPN"
        const val ACTION_STOP  = "com.moweapp.antonio.STOP_VPN"

        /** Broadcast sent to update the UI with the current block count. */
        const val BROADCAST_STATS = "com.moweapp.antonio.VPN_STATS"
        const val EXTRA_BLOCKED_COUNT = "blocked_count"

        private const val NOTIFICATION_ID      = 1
        private const val NOTIFICATION_CHANNEL = "vpn_channel"
        private const val TAG = "MyVpnService"

        // TUN interface parameters — RFC 5737 documentation address range
        private const val TUN_ADDRESS = "10.0.0.2"
        private const val TUN_PREFIX  = 32
        private const val TUN_ROUTE   = "0.0.0.0"
        private const val TUN_MTU     = 1500
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private var tunInterface: ParcelFileDescriptor? = null
    private var processorJob: Job? = null
    private var processor: PacketProcessor? = null

    private val filterEngine = DomainFilterEngine()
    private val blockedCount = AtomicInteger(0)
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP  -> stopVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    // ── VPN start/stop ────────────────────────────────────────────────────────

    private fun startVpn() {
        if (tunInterface != null) {
            Log.w(TAG, "VPN already running — ignoring start")
            return
        }

        // 1. Load blocklist in background (engine fails-open until ready)
        serviceScope.launch {
            val domains = BlocklistRepository.loadDomains(applicationContext)
            filterEngine.loadAsync(domains)
        }

        // 2. Build TUN interface
        val tun = buildTunInterface() ?: run {
            Log.e(TAG, "Failed to build TUN interface")
            return
        }
        tunInterface = tun

        // 3. Start foreground notification
        startForeground(NOTIFICATION_ID, buildNotification("Ad blocker active"))

        // 4. Spin up packet processing on IO thread
        val input  = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)

        processor = PacketProcessor(
            vpnInput     = input,
            vpnOutput    = output,
            filterEngine = filterEngine,
            onDomainBlocked = { domain ->
                val count = blockedCount.incrementAndGet()
                broadcastStats(count)
                Log.d(TAG, "Blocked[$count]: $domain")
            }
        )

        processorJob = serviceScope.launch {
            processor?.start()   // blocks until stop() is called
        }

        Log.i(TAG, "VPN started")
    }

    private fun stopVpn() {
        processor?.stop()
        processorJob?.cancel()
        processorJob = null
        processor = null

        tunInterface?.close()
        tunInterface = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.i(TAG, "VPN stopped")
    }

    // ── TUN interface builder ─────────────────────────────────────────────────

    private fun buildTunInterface(): ParcelFileDescriptor? {
        return try {
            Builder()
                .setSession("MoweApp VPN")
                .addAddress(TUN_ADDRESS, TUN_PREFIX)
                .addRoute(TUN_ROUTE, 0)               // route all traffic through tun
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
                .setMtu(TUN_MTU)
                .setBlocking(true)
                .establish()
        } catch (e: Exception) {
            Log.e(TAG, "TUN establish failed: ${e.message}")
            null
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(text: String): Notification {
        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setContentTitle("MoweApp VPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                "VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "MoweApp VPN ad blocker status" }

            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    // ── Stats broadcast ───────────────────────────────────────────────────────

    private fun broadcastStats(count: Int) {
        val intent = Intent(BROADCAST_STATS).apply {
            putExtra(EXTRA_BLOCKED_COUNT, count)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
