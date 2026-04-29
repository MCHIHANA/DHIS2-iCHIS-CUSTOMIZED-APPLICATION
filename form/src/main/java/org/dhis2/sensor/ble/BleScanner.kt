package org.dhis2.sensor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID

private const val TAG = "BLE_SCAN"

/** Standard BLE Health Thermometer service UUID (Bluetooth SIG assigned). */
private val HEALTH_THERMOMETER_SERVICE_UUID =
    ParcelUuid(UUID.fromString("00001809-0000-1000-8000-00805f9b34fb"))

@SuppressLint("MissingPermission")
class BleScanner(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private var scanCallback: ScanCallback? = null

    /**
     * Starts a continuous BLE scan filtered to the **Health Thermometer service**
     * (UUID 0x1809). Only devices advertising this service will trigger
     * [onTargetFound] — no name guessing or MAC matching needed.
     *
     * Scan mode is LOW_LATENCY so the sensor is detected as fast as possible
     * during its brief advertisement window.
     *
     * The scan runs until a match is found or [stopScan] is called manually
     * (e.g. when the user dismisses the dialog).
     *
     * [onDeviceFound] is still called for every matched device so the device-list
     * UI keeps working.
     */
    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
        // kept for API compatibility — not used in this implementation
        onAdvertisementTemperature: ((Double) -> Unit)? = null,
    ) {
        if (scanCallback != null) return // already scanning

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(HEALTH_THERMOMETER_SERVICE_UUID)
                .build(),
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                Log.d("BLE_MATCH", "Thermometer detected: ${device.address}")

                onDeviceFound(device)
                stopScan()
                onTargetFound?.invoke(device)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error code: $errorCode")
            }
        }

        scanCallback = callback
        scanner?.startScan(filters, settings, callback)
        Log.d(TAG, "Scanning for Health Thermometer service (UUID 0x1809)...")
    }

    /** Stops the scan. Called automatically after a match or manually on dismiss. */
    fun stopScan() {
        scanCallback?.let {
            scanner?.stopScan(it)
            scanCallback = null
            Log.d(TAG, "Scan stopped after match")
        }
    }
}
