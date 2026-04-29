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
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private val seenDevices = mutableListOf<BluetoothDevice>()
    private var scanCallback: ScanCallback? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())

    /**
     * Starts a BLE scan.
     *
     * When any device in [KnownDevices.ALL] is detected the scan stops immediately and
     * [onTargetFound] is invoked for automatic connection.
     * Every discovered device is also reported via [onDeviceFound] so existing device-list
     * UI keeps working.
     * The scan auto-stops after [SCAN_TIMEOUT_MS] ms if no known device appears.
     */
    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
    ) {
        if (scanCallback != null) return // already scanning

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val address = device.address

                Log.d(TAG, "Found device: $address")

                if (!seenDevices.contains(device)) {
                    seenDevices.add(device)
                    onDeviceFound(device)
                }

                if (address in KnownDevices.ALL) {
                    Log.d(TAG, "Known sensor detected: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                }
            }
        }

        scanCallback = callback
        scanner?.startScan(callback)
        Log.d(TAG, "Scan started")

        // Auto-stop after timeout to prevent infinite scanning
        timeoutHandler.postDelayed({
            if (scanCallback != null) {
                stopScan()
                Log.d(TAG, "Scan timeout after ${SCAN_TIMEOUT_MS / 1000}s — no known sensor found")
            }
        }, SCAN_TIMEOUT_MS)
    }

    fun stopScan() {
        timeoutHandler.removeCallbacksAndMessages(null)
        scanCallback?.let {
            scanner?.stopScan(it)
            scanCallback = null
            Log.d(TAG, "Scan stopped")
        }
    }
}
