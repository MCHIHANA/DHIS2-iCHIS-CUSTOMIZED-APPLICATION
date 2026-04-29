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
     * For FORA sensors the temperature is read directly from the advertisement
     * packet — no GATT connection is needed. The flow is:
     *
     *   1. User taps Temperature field → scan starts
     *   2. User turns ON the FORA IR42
     *   3. Sensor broadcasts an advertisement containing the temperature
     *   4. [onAdvertisementTemperature] is called with the parsed value
     *   5. Scan stops — done, no GATT connection
     *
     * If the advertisement does not contain a parseable temperature (e.g. a
     * different FORA firmware or a non-FORA known-MAC device), [onTargetFound]
     * is called instead so the existing GATT path takes over as a fallback.
     *
     * [onDeviceFound] is called for every new device (device-list UI).
     */
    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
        onAdvertisementTemperature: ((Double) -> Unit)? = null,
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

                val isFora = name.contains("FORA", ignoreCase = true)
                val isKnownMac = address in KnownDevices.ALL

                if (!isFora && !isKnownMac) return

                // ── FORA advertisement path ───────────────────────────────
                if (isFora) {
                    Log.d("BLE_MATCH", "FORA advertisement detected: $name ($address)")

                    val scanRecord = result.scanRecord
                    val manufacturerData = scanRecord?.getManufacturerSpecificData()

                    if (manufacturerData != null) {
                        for (i in 0 until manufacturerData.size()) {
                            val bytes = manufacturerData.valueAt(i)
                            Log.d("BLE_ADV", "Device: $name | key=${manufacturerData.keyAt(i)}")

                            val temperature = BleDataParser.parseAdvertisementTemperature(bytes)
                            if (temperature != null) {
                                Log.d("BLE_TEMP", "Temperature = $temperature °C")
                                stopScan()
                                Log.d("BLE_RESULT", "Temperature ready: $temperature")
                                onAdvertisementTemperature?.invoke(temperature)
                                return // done — no GATT needed
                            }
                        }
                    }

                    // Advertisement didn't contain a parseable temperature —
                    // fall through to GATT connection as backup
                    Log.d(TAG, "No temperature in advertisement — falling back to GATT")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }

                // ── Known-MAC path (non-FORA) → GATT connection ──────────
                Log.d("BLE_MATCH", "Known MAC detected: $address")
                stopScan()
                onTargetFound?.invoke(device)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error code: $errorCode")
            }
        }

        scanCallback = callback
        scanner?.startScan(null, settings, callback)
        Log.d(TAG, "Starting continuous scan...")
    }

    /** Stops the scan. Called after a match or manually on dismiss. */
    fun stopScan() {
        scanCallback?.let {
            scanner?.stopScan(it)
            scanCallback = null
            Log.d(TAG, "Scan stopped after match")
        }
    }
}
