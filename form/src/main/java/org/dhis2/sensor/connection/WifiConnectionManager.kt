package org.dhis2.sensor.connection

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log

class WifiConnectionManager(
    private val context: Context
) {

    private var onDeviceFound: ((ScanResult) -> Unit)? = null
    private var onNoDeviceFound: (() -> Unit)? = null

    fun setCallbacks(
        onDeviceFound: (ScanResult) -> Unit,
        onNoDeviceFound: () -> Unit
    ) {
        this.onDeviceFound = onDeviceFound
        this.onNoDeviceFound = onNoDeviceFound
    }

    fun connectWifi() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        try {
            wifiManager.startScan()
            val results = wifiManager.scanResults
            if (results.isEmpty()) {
                showWifiNotFound()
            } else {
                connectWifiDevice(results)
            }
        } catch (e: SecurityException) {
            Log.e("WifiConnectionManager", "Missing location permission for WiFi scan", e)
            showWifiNotFound()
        }
    }

    fun scanForDevices(): List<ScanResult> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            wifiManager.scanResults
        } catch (e: SecurityException) {
            Log.e("WifiConnectionManager", "Missing location permission for WiFi scan", e)
            emptyList()
        }
    }

    private fun showWifiNotFound() {
        Log.d("WifiConnectionManager", "No WiFi devices found")
        onNoDeviceFound?.invoke()
    }

    private fun connectWifiDevice(results: List<ScanResult>) {
        val device = results.firstOrNull()
        if (device != null) {
            Log.d("WifiConnectionManager", "Connecting to WiFi device: ${device.SSID}")
            onDeviceFound?.invoke(device)
        } else {
            onNoDeviceFound?.invoke()
        }
    }
}
