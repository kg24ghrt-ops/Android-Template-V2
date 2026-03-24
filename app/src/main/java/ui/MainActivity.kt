package com.moweapp.antonio.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.moweapp.antonio.R
import com.moweapp.antonio.browser.BrowserLauncher
import com.moweapp.antonio.vpn.MyVpnService

/**
 * MainActivity
 *
 * Simple, single-screen UI for the MoweApp VPN ad blocker.
 *
 * Controls:
 *  - Start / Stop VPN toggle
 *  - Live blocked domain count (updated via [BroadcastReceiver])
 *  - "Open vidbox.cc" browser launcher button
 *
 * Layout file: res/layout/activity_main.xml
 * Required IDs: tv_status, tv_blocked_count, btn_start, btn_stop, btn_open_browser
 *
 * No ViewModel, no LiveData — state is held in simple member fields and
 * reconstructed from the VPN service state on resume.
 */
class MainActivity : AppCompatActivity() {

    // ── Views ─────────────────────────────────────────────────────────────────

    private lateinit var tvStatus: TextView
    private lateinit var tvBlockedCount: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnOpenBrowser: Button

    // ── State ─────────────────────────────────────────────────────────────────

    private var vpnRunning = false
    private var blockedCount = 0

    // ── VPN permission launcher ───────────────────────────────────────────────

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                launchVpnService()
            }
            // If denied, do nothing — user chose not to allow
        }

    // ── Stats broadcast receiver ──────────────────────────────────────────────

    private val statsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val count = intent?.getIntExtra(MyVpnService.EXTRA_BLOCKED_COUNT, 0) ?: return
            blockedCount = count
            updateBlockedCount()
        }
    }

    // ── Activity lifecycle ────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupClickListeners()
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        registerStatsReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statsReceiver)
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private fun bindViews() {
        tvStatus       = findViewById(R.id.tv_status)
        tvBlockedCount = findViewById(R.id.tv_blocked_count)
        btnStart       = findViewById(R.id.btn_start)
        btnStop        = findViewById(R.id.btn_stop)
        btnOpenBrowser = findViewById(R.id.btn_open_browser)
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener  { onStopClicked()  }
        btnOpenBrowser.setOnClickListener { onOpenBrowserClicked() }
    }

    private fun onStartClicked() {
        // VpnService.prepare() returns an intent if the user needs to grant
        // permission. If null is returned, permission is already granted.
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent != null) {
            vpnPermissionLauncher.launch(permissionIntent)
        } else {
            launchVpnService()
        }
    }

    private fun onStopClicked() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_STOP
        }
        startService(intent)

        vpnRunning = false
        blockedCount = 0
        updateUi()
    }

    private fun onOpenBrowserClicked() {
        BrowserLauncher.open(this)
    }

    // ── VPN service launch ────────────────────────────────────────────────────

    private fun launchVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        vpnRunning = true
        updateUi()
    }

    // ── Broadcast receiver registration ───────────────────────────────────────

    private fun registerStatsReceiver() {
        val filter = IntentFilter(MyVpnService.BROADCAST_STATS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statsReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statsReceiver, filter)
        }
    }

    // ── UI updates ────────────────────────────────────────────────────────────

    private fun updateUi() {
        updateStatus()
        updateBlockedCount()
        updateButtons()
    }

    private fun updateStatus() {
        tvStatus.text = if (vpnRunning) "● VPN ON" else "○ VPN OFF"
        tvStatus.setTextColor(
            if (vpnRunning)
                getColor(android.R.color.holo_green_dark)
            else
                getColor(android.R.color.holo_red_dark)
        )
    }

    private fun updateBlockedCount() {
        tvBlockedCount.text = "Blocked: $blockedCount domains"
    }

    private fun updateButtons() {
        btnStart.visibility = if (vpnRunning) View.GONE else View.VISIBLE
        btnStop.visibility  = if (vpnRunning) View.VISIBLE else View.GONE
    }
}
