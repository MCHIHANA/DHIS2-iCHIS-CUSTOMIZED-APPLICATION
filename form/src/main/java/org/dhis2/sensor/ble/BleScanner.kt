package org.dhis2.sensor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log

private const val TAG = "BLE_SCAN"

@SuppressLint("MissingPermission")
class BleScanner(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private val seenDevices = mutableListOf<BluetoothDevice>()
    private var scanCallback: ScanCallback? = null

    /**
     * Starts a **continuous** BLE scan using LOW_LATENCY mode.
     *
     * The scan runs indefinitely — it does NOT stop on a timer.
     * It stops only when a target device is found:
     *   - Any MAC in [KnownDevices.ALL], OR
     *   - Any device whose advertised name contains "FORA" (case-insensitive),
     *     which covers the FORA IR42 thermometer and other FORA-branded sensors.
     *
     * Workflow:
     *   1. User taps Temperature field → scan starts
     *   2. User turns ON the sensor
     *   3. Sensor broadcasts briefly → app detects it immediately
     *   4. Scan stops, connection begins
     *
     * [onDeviceFound] is called for every new device (for device-list UI).
     * [onTargetFound] is called once when the target sensor is detected.
     */
    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
    ) {
        if (scanCallback != null) return // already scanning

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val address = device.address
                val name = device.name ?: "Unknown"

                Log.d("BLE_DEVICE", "Found → $name ($address)")

                // Surface every new device to the UI list
                if (!seenDevices.contains(device)) {
                    seenDevices.add(device)
                    onDeviceFound(device)
                }

                // Match by known MAC address
                if (address in KnownDevices.ALL) {
                    Log.d("BLE_MATCH", "Known MAC detected: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }

                // Match by device name — catches FORA IR42 and other FORA sensors
                if (name.contains("FORA", ignoreCase = true)) {
                    Log.d("BLE_MATCH", "FORA sensor detected: $name ($address)")
                    stopScan()
                    onTargetFound?.invoke(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error code: $errorCode")
            }
        }

        scanCallback = callback
        // No filters — scan all devices so name-based matching works
        scanner?.startScan(null, settings, callback)
        Log.d(TAG, "Starting continuous scan...")
    }

    /** Stops the scan. Called automatically after a match, or manually on dismiss/disconnect. */
    fun stopScan() {
        scanCallback?.let {
            scanner?.stopScan(it)
            scanCallback = null
            Log.d(TAG, "Scan stopped after match")
        }
    }
}
