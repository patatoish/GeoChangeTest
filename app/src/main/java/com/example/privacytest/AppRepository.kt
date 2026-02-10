package com.example.privacytest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppRepository {
    
    // VPN State
    data class VpnState(val isConnected: Boolean = false, val ip: String = "Unknown", val country: String = "Unknown")
    private val _vpnState = MutableStateFlow(VpnState())
    val vpnState = _vpnState.asStateFlow()

    // DNS State
    data class DnsState(val provider: String = "Default", val resolverIp: String = "Unknown")
    private val _dnsState = MutableStateFlow(DnsState())
    val dnsState = _dnsState.asStateFlow()

    fun updateVpnStatus(isConnected: Boolean, ip: String, country: String) {
        _vpnState.value = VpnState(isConnected, ip, country)
    }

    fun updateDnsStatus(provider: String, resolverIp: String) {
        _dnsState.value = DnsState(provider, resolverIp)
    }

    // Mock Config presence
    var hasRealWireGuardConfig: Boolean = false
}
