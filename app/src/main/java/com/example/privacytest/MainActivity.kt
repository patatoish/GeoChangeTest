package com.example.privacytest

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ArrayAdapter
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import com.example.privacytest.LocationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var locationHelper: LocationHelper

    private lateinit var switchVpn: SwitchMaterial
    private lateinit var tvVpnStatus: TextView
    private lateinit var btnImport: Button

    private lateinit var switchDnsVpn: SwitchMaterial
    private lateinit var spinnerDns: Spinner
    private lateinit var tvDnsStatus: TextView

    private lateinit var switchMockLocation: SwitchMaterial
    private lateinit var tvMockProviderStatus: TextView
    private lateinit var tvRealLatLon: TextView
    private lateinit var tvMockLatLon: TextView
    private lateinit var tvRealAcc: TextView
    private lateinit var tvMockAcc: TextView

    private lateinit var btnSelfTest: Button
    private lateinit var tvLog: TextView

    private val vpnPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "VPN Permission Denied", Toast.LENGTH_SHORT).show()
            switchVpn.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationHelper = LocationHelper(this)

        initViews()
        setupListeners()
        observeState()
    }

    private fun initViews() {
        switchVpn = findViewById(R.id.switchVpn)
        tvVpnStatus = findViewById(R.id.tvVpnStatus)
        btnImport = findViewById(R.id.btnImportConfig)

        spinnerDns = findViewById(R.id.spinnerDns)
        switchDnsVpn = findViewById(R.id.switchDnsVpn)
        tvDnsStatus = findViewById(R.id.tvDnsStatus)

        // Setup DNS Spinner
        val dnsOptions = arrayOf("Cloudflare (1.1.1.1)", "Google (8.8.8.8)", "NextDNS", "Custom")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dnsOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDns.adapter = adapter

        switchMockLocation = findViewById(R.id.switchMockLocation)
        tvMockProviderStatus = findViewById(R.id.tvMockProviderStatus)
        tvRealLatLon = findViewById(R.id.tvRealLatLon)
        tvMockLatLon = findViewById(R.id.tvMockLatLon)
        tvRealAcc = findViewById(R.id.tvRealAcc)
        tvMockAcc = findViewById(R.id.tvMockAcc)

        btnSelfTest = findViewById(R.id.btnSelfTest)
        tvLog = findViewById(R.id.tvLog)
    }

    private fun setupListeners() {
        switchVpn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                prepareVpn()
            } else {
                stopVpnService()
            }
        }
        
        btnImport.setOnClickListener {
           // File Picker for .conf
           val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // WireGuard files might not have a specific mime type, or text/plain
           }
           filePickerLauncher.launch(intent)
        }

        switchMockLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkLocationPermissions()
            } else {
                locationHelper.stopLocationUpdates()
                locationHelper.disableMockProvider()
            }
        }
        
        btnSelfTest.setOnClickListener {
             runSelfTest()
        }
        
        spinnerDns.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString()
                // Update Repository or Service
                if (switchDnsVpn.isChecked) {
                     // In real app, restart VPN with new DNS
                     Toast.makeText(this@MainActivity, "DNS set to $selected (Requires VPN restart)", Toast.LENGTH_SHORT).show()
                }
                // Mock update
                val resolverIp = when(position) {
                    0 -> "1.1.1.1"
                    1 -> "8.8.8.8"
                    2 -> "45.90.28.0"
                    else -> "192.168.1.1"
                }
                AppRepository.updateDnsStatus(selected, resolverIp)
                tvDnsStatus.text = "Active: $selected ($resolverIp)"
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
    
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                // Mock Import
                Toast.makeText(this, "Imported Config: ${uri.path}", Toast.LENGTH_LONG).show()
                AppRepository.hasRealWireGuardConfig = true
            }
        }
    }
    
    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }
    
    private fun startVpnService() {
        val intent = Intent(this, MyVpnService::class.java)
        // Pass fake config info if needed
        startService(intent)
    }
    
    private fun stopVpnService() {
        val intent = Intent(this, MyVpnService::class.java)
        intent.action = MyVpnService.ACTION_STOP
        startService(intent)
    }

    private fun checkLocationPermissions() {
         if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
             switchMockLocation.isChecked = false
             return
         }
         
         // Start updates
         locationHelper.startLocationUpdates()
         
         // Try to enable mock
         // Show dialog sending user to dev options if needed?
         // For now, just try enabling
         locationHelper.enableMockProvider()
    }

    private fun observeState() {
        lifecycleScope.launch {
            AppRepository.vpnState.collect { state ->
                val statusText = if (state.isConnected) "Connected: ${state.ip} (${state.country})" else "Disconnected"
                tvVpnStatus.text = statusText
                // Prevent infinite loop if state updated from service vs switch
                if (switchVpn.isChecked != state.isConnected) {
                    // switchVpn.isChecked = state.isConnected // Better not to force unless reliable
                }
            }
        }
        
        lifecycleScope.launch {
             locationHelper.locationState.collect { state ->
                  if (state.realLocation != null) {
                      tvRealLatLon.text = "${state.realLocation.latitude}, ${state.realLocation.longitude}"
                      tvRealAcc.text = "${state.realLocation.accuracy}m"
                  } else {
                      tvRealLatLon.text = "N/A"
                  }
                  
                  if (state.mockLocation != null) {
                      tvMockLatLon.text = "${state.mockLocation.latitude}, ${state.mockLocation.longitude}"
                      tvMockAcc.text = "${state.mockLocation.accuracy}m"
                  } else {
                       tvMockLatLon.text = "N/A"
                  }
                  
                  if (state.isMockProviderActive) {
                      tvMockProviderStatus.text = "Active Mock Provider"
                      tvMockProviderStatus.setBackgroundColor(0xFFC8E6C9.toInt()) // Light Green
                      tvMockProviderStatus.setTextColor(0xFF2E7D32.toInt()) // Dark Green
                  } else {
                      tvMockProviderStatus.text = "Not Selected"
                      tvMockProviderStatus.setBackgroundColor(0xFFFFCDD2.toInt()) // Light Red
                      tvMockProviderStatus.setTextColor(0xFFB71C1C.toInt()) // Dark Red
                  }
             }
        }
    }
    
    private fun runSelfTest() {
        val report = SelfTest.run(this, AppRepository.vpnState.value, locationHelper.locationState.value)
        tvLog.text = report
    }
}
