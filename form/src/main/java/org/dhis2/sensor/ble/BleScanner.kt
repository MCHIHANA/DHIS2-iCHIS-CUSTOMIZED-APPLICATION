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

/**
 * Health Thermometer service UUID — used for GATT verification after connection,
 * NOT as a scan filter (many devices don't include it in their advertisement packet).
 */
private const val HEALTH_THERMOMETER_UUID = "00001809-0000-1000-8000-00805f9b34fb"

@SuppressLint("MissingPermission")
class BleScanner(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private val seenDevices = mutableSetOf<String>() // MACs already reported
    private var scanCallback: ScanCallback? = null

    /**
     * Starts a **continuous unfiltered** BLE scan using LOW_LATENCY mode.
     *
     * Why unfiltered: many BLE thermometers (including FORA IR42) do NOT include
     * the Health Thermometer service UUID (0x1809) in their advertisement packet —
     * they only expose it after a GATT connection. A service-UUID filter at the OS
     * level would therefore never match them.
     *
     * Match logic (first match wins, scan stops):
     *   1. Known MAC address in [KnownDevices.ALL]
     *   2. Device name contains "FORA" (case-insensitive)
     *   3. Device advertises service UUID 0x1809 in its scan record
     *
     * Every discovered device is also logged under BLE_DEVICE so you can see
     * exactly what the thermometer is advertising when it powers on.
     *
     * [onDeviceFound] — called for every new device (device-list UI).
     * [onTargetFound] — called once when a match is found; scan stops.
     */
    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
        onAdvertisementTemperature: ((Double) -> Unit)? = null, // API compat
    ) {
        if (scanCallback != null) return // already scanning
        seenDevices.clear()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val address = device.address
                val name = device.name ?: ""

                // Log every device once so we can see what the thermometer broadcasts
                if (seenDevices.add(address)) {
                    val serviceUuids = result.scanRecord?.serviceUuids
                        ?.joinToString { it.uuid.toString() } ?: "none"
                    Log.d(
                        "BLE_DEVICE",
                        "Found: name='$name' mac=$address services=[$serviceUuids]",
                    )
                    onDeviceFound(device)
                }

                // ── Match criteria ────────────────────────────────────────────

                // 1. Known MAC
                if (address in KnownDevices.ALL) {
                    Log.d("BLE_MATCH", "Known MAC matched: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }

                // 2. FORA device name
                if (name.contains("FORA", ignoreCase = true)) {
                    Log.d("BLE_MATCH", "FORA name matched: '$name' ($address)")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }

                // 3. Health Thermometer service UUID in advertisement
                val advertisedServices = result.scanRecord?.serviceUuids
                if (advertisedServices != null &&
                    advertisedServices.any {
                        it.uuid.toString().equals(HEALTH_THERMOMETER_UUID, ignoreCase = true)
                    }
                ) {
                    Log.d("BLE_MATCH", "Health Thermometer service UUID matched: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed — error code: $errorCode")
            }
        }

        scanCallback = callback
        // null filters = scan all devices; we do our own matching above
        scanner?.startScan(null, settings, callback)
        Log.d(TAG, "Starting continuous unfiltered scan (LOW_LATENCY)...")
    }

    /** Stops the scan. Called after a match or manually on dismiss. */
    fun stopScan() {
        scanCallback?.let {
            scanner?.stopScan(it)
            scanCallback = null
            Log.d(TAG, "Scan stopped")
        }
    }
}
