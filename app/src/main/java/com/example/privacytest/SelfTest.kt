package com.example.privacytest

import android.content.Context
import org.json.JSONObject
import java.util.Date

object SelfTest {
    
    fun run(context: Context, vpnState: AppRepository.VpnState, locationState: LocationHelper.LocationState): String {
        val report = JSONObject()
        
        try {
            report.put("timestamp", Date().toString())
            
            // VPN Check
            val vpnJson = JSONObject()
            vpnJson.put("connected", vpnState.isConnected)
            vpnJson.put("ip", vpnState.ip)
            vpnJson.put("country", vpnState.country)
            report.put("vpn_status", vpnJson)
            
            // Location Check
            val locJson = JSONObject()
            locJson.put("mock_provider_active", locationState.isMockProviderActive)
            locJson.put("real_lat", locationState.realLocation?.latitude ?: "N/A")
            locJson.put("mock_lat", locationState.mockLocation?.latitude ?: "N/A")
            report.put("location_status", locJson)
            
            // DNS Check (Mock Simulation)
            val dnsJson = JSONObject()
            dnsJson.put("resolved_host", "example.com")
            dnsJson.put("resolved_ip", if(vpnState.isConnected) "93.184.216.34" else "System Resolved")
            report.put("dns_test", dnsJson)
            
        } catch (e: Exception) {
            report.put("error", e.message)
        }
        
        return report.toString(4)
    }
}
