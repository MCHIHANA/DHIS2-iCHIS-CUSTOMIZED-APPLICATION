package org.dhis2.sensor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

private const val SCAN_TIMEOUT_MS = 15_000L
private const val TAG = "BLE_SCAN"

@SuppressLint("MissingPermission")
class BleScanner(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private val devices = mutableListOf<BluetoothDevice>()
    private var scanCallback: ScanCallback? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())

    /**
     * Starts a BLE scan. When the target device ([TargetDeviceConfig.TEMPERATURE_SENSOR_MAC])
     * is found, [onTargetFound] is invoked and the scan stops automatically.
     * The scan also stops after [SCAN_TIMEOUT_MS] milliseconds if the device is not found.
     *
     * [onDeviceFound] is still called for every discovered device so the existing device-list
     * UI continues to work.
     */
    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
    ) {
        if (scanCallback != null) return // Already scanning

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val address = device.address

                Log.d(TAG, "Found device: $address")

                if (!devices.contains(device)) {
                    devices.add(device)
                    onDeviceFound(device)
                }

                if (address == TargetDeviceConfig.TEMPERATURE_SENSOR_MAC) {
                    Log.d(TAG, "Target device found!")
                    stopScan()
                    onTargetFound?.invoke(device)
                }
            }
        }

        scanCallback = callback
        scanner?.startScan(callback)

        // Auto-stop after timeout to prevent infinite scanning
        timeoutHandler.postDelayed({
            if (scanCallback != null) {
                stopScan()
                Log.d(TAG, "Scan timeout after ${SCAN_TIMEOUT_MS / 1000} seconds")
            }
        }, SCAN_TIMEOUT_MS)
    }

    fun stopScan() {
        timeoutHandler.removeCallbacksAndMessages(null)
        scanCallback?.let {
            scanner?.stopScan(it)
            scanCallback = null
        }
    }
}
