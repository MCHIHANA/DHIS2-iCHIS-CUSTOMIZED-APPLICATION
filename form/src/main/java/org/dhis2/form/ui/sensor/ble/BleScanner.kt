package org.dhis2.form.ui.sensor.ble

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

class BleScanner(context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private val handler = Handler(Looper.getMainLooper())

    private var isScanning = false

    @SuppressLint("MissingPermission")
    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (isScanning) return

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                result.device?.let { device ->
                    onDeviceFound(device)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    result.device?.let { device ->
                        onDeviceFound(device)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLE_SCANNER", "Scan failed with error: $errorCode")
            }
        }

        isScanning = true
        scanner?.startScan(scanCallback)

        // Stop scanning after a pre-defined scan period.
        handler.postDelayed({
            stopScan(scanCallback)
        }, SCAN_PERIOD)
    }

    @SuppressLint("MissingPermission")
    fun stopScan(callback: ScanCallback) {
        if (!isScanning) return
        isScanning = false
        scanner?.stopScan(callback)
    }

    companion object {
        private const val SCAN_PERIOD: Long = 10000
    }
}
