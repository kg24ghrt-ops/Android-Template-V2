package com.moweapp.antonio.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.moweapp.antonio.databinding.ActivityMainBinding
import com.moweapp.antonio.engine.BrowserEngine
import com.moweapp.antonio.engine.PerformanceManager
import com.moweapp.antonio.engine.RequestInterceptor
import com.moweapp.antonio.vpn.DomainFilterEngine
import com.moweapp.antonio.vpn.MyVpnService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val browserEngine = BrowserEngine()
    private val filterEngine = DomainFilterEngine()

    private var vpnRunning = false
    private var blockedCount = 0

    // ── VPN permission launcher ───────────────────────────────────────────────
    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) launchVpnService()
        }

    // ── Stats broadcast receiver ──────────────────────────────────────────────
    private val statsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            blockedCount = intent?.getIntExtra(MyVpnService.EXTRA_BLOCKED_COUNT, 0) ?: return
            updateBlockedCount()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Core Browser Setup
        setupBrowserEngine()
        
        // 2. Click Listeners
        setupClickListeners()
        
        // 3. Initial UI State
        updateUi()
    }

    private fun setupBrowserEngine() {
        // Connect the ad-blocker to the browser
        val interceptor = RequestInterceptor(filterEngine) { newUrl ->
            runOnUiThread { binding.urlEditText.setText(newUrl) }
        }

        // Connect the progress bar
        val performanceManager = PerformanceManager(
            onProgressUpdate = { progress ->
                binding.browserProgress.progress = progress
            },
            onLoadingStateChanged = { isLoading ->
                binding.browserProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        )

        // Start GeckoView
        browserEngine.init(this, binding.geckoview, interceptor, performanceManager)
    }

    private fun setupClickListeners() {
        binding.btnStart.setOnClickListener { onStartClicked() }
        binding.btnStop.setOnClickListener { onStopClicked() }

        // SWITCH: Dashboard -> Browser
        binding.btnOpenBrowser.setOnClickListener {
            binding.dashboardContainer.visibility = View.GONE
            binding.browserContainer.visibility = View.VISIBLE
            browserEngine.loadUrl("https://vidbox.cc")
        }

        // URL bar "Enter" key logic
        binding.urlEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                browserEngine.loadUrl(binding.urlEditText.text.toString())
                binding.urlEditText.clearFocus()
                true
            } else false
        }
    }

    // ── VPN Controls ──────────────────────────────────────────────────────────

    private fun onStartClicked() {
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent != null) vpnPermissionLauncher.launch(permissionIntent)
        else launchVpnService()
    }

    private fun onStopClicked() {
        startService(Intent(this, MyVpnService::class.java).apply { action = MyVpnService.ACTION_STOP })
        vpnRunning = false
        blockedCount = 0
        updateUi()
    }

    private fun launchVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply { action = MyVpnService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        vpnRunning = true
        updateUi()
    }

    // ── Navigation Logic ──────────────────────────────────────────────────────

    override fun onBackPressed() {
        // If we are in the browser, handle history. If at start of history, go back to dashboard.
        if (binding.browserContainer.visibility == View.VISIBLE) {
            if (browserEngine.canGoBack()) {
                browserEngine.goBack()
            } else {
                binding.browserContainer.visibility = View.GONE
                binding.dashboardContainer.visibility = View.VISIBLE
            }
        } else {
            super.onBackPressed()
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private fun updateUi() {
        binding.tvStatus.text = if (vpnRunning) "● VPN ON" else "○ VPN OFF"
        binding.tvStatus.setTextColor(getColor(if (vpnRunning) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        updateBlockedCount()
        binding.btnStart.visibility = if (vpnRunning) View.GONE else View.VISIBLE
        binding.btnStop.visibility = if (vpnRunning) View.VISIBLE else View.GONE
    }

    private fun updateBlockedCount() {
        binding.tvBlockedCount.text = "Blocked: $blockedCount domains"
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(MyVpnService.BROADCAST_STATS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statsReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statsReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(statsReceiver)
    }
}
