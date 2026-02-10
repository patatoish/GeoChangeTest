package com.example.privacytest

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        
        startVpn()
        return START_STICKY
    }
    
    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
            builder.addAddress("10.0.0.2", 32)
            builder.addRoute("0.0.0.0", 0)
            builder.setSession("PrivacyTestMockVPN")
            builder.setMtu(1280)
            
            // Add DNS servers if configured
            // builder.addDnsServer("1.1.1.1") 

            vpnInterface = builder.establish()
            Log.i("MyVpnService", "Mock VPN Established")
            
            // Update Repository
            AppRepository.updateVpnStatus(true, "203.0.113.45 (Mock)", "US (Mock)")
            
        } catch (e: Exception) {
            Log.e("MyVpnService", "Error starting VPN", e)
            stopSelf()
        }
    }
    
    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            AppRepository.updateVpnStatus(false, "Unknown", "Unknown")
        } catch (e: Exception) {
            Log.e("MyVpnService", "Error stopping VPN", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    companion object {
        const val ACTION_STOP = "com.example.privacytest.STOP_VPN"
    }
}
