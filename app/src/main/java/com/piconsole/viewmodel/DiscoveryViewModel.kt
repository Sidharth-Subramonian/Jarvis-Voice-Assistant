package com.piconsole.viewmodel

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

data class DiscoveredDevice(
    val name: String,
    val ip: String,
    val model: String
)

class DiscoveryViewModel : ViewModel() {
    private var nsdManager: NsdManager? = null
    
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun initNsd(context: Context) {
        if (nsdManager == null) {
            nsdManager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            _isScanning.value = true
        }
        override fun onServiceFound(service: NsdServiceInfo) {
            if (service.serviceType.contains("_piconsole._tcp")) {
                nsdManager?.resolveService(service, resolveListener)
            }
        }
        override fun onServiceLost(service: NsdServiceInfo) { }
        override fun onDiscoveryStopped(serviceType: String) {
            _isScanning.value = false
        }
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            try { nsdManager?.stopServiceDiscovery(this) } catch(e: Exception) {}
            _isScanning.value = false
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            try { nsdManager?.stopServiceDiscovery(this) } catch(e: Exception) {}
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e("Discovery", "Resolve failed: $errorCode")
        }
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val name = serviceInfo.serviceName
            val ip = serviceInfo.host.hostAddress
            
            var model = "Raspberry Pi"
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    serviceInfo.attributes["model"]?.let {
                        model = String(it, StandardCharsets.UTF_8)
                    }
                }
            } catch (e: Exception) {}
            
            if (ip != null) {
                val newDevice = DiscoveredDevice(name, ip, model)
                if (!_devices.value.any { it.ip == ip }) {
                    _devices.value = _devices.value + newDevice
                }
            }
        }
    }

    fun startDiscovery() {
        if (nsdManager == null) {
            Log.e("Discovery", "NsdManager not initialized. Call initNsd(context) first.")
            return
        }
        try {
            _devices.value = emptyList()
            nsdManager?.discoverServices("_piconsole._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("Discovery", "Failed to start discovery", e)
        }
    }

    fun stopDiscovery() {
        try {
            nsdManager?.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) { }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
