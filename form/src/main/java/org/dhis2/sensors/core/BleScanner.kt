package org.dhis2.sensors.core

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import org.dhis2.sensors.devices.bloodpressure.BloodPressureConstants
import org.dhis2.sensors.devices.spo2.Spo2Constants
import org.dhis2.sensors.devices.temperature.TemperatureConstants
import org.dhis2.sensors.utils.SensorLogger

@SuppressLint("MissingPermission")
class BleScanner(
    private val context: Context,
) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private val seenDevices = mutableSetOf<String>()
    private var scanCallback: ScanCallback? = null

    fun startScan(
        preferredTargetAddress: String? = null,
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
        onAdvertisementTemperature: ((Double) -> Unit)? = null,
        onScanFailed: ((Int) -> Unit)? = null,
    ) {
        if (scanCallback != null) {
            SensorLogger.w(TAG, "Scan already in progress, restarting")
            stopScan()
        }
        if (bluetoothAdapter == null) {
            SensorLogger.e(TAG, "Bluetooth adapter is null")
            return
        }
        if (scanner == null) {
            SensorLogger.e(TAG, "BLE scanner is null")
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            SensorLogger.e(TAG, "Bluetooth is disabled")
            return
        }

        seenDevices.clear()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult,
            ) {
                val device = result.device ?: return
                val address = device.address
                val name = device.name ?: ""

                if (seenDevices.add(address)) {
                    val services = result.scanRecord?.serviceUuids?.joinToString { it.uuid.toString() } ?: "none"
                    SensorLogger.d("BLE_DEVICE", "Found: name='$name' mac=$address services=[$services]")
                    onDeviceFound(device)
                }

                if (preferredTargetAddress != null &&
                    address.equals(preferredTargetAddress, ignoreCase = true)
                ) {
                    SensorLogger.d("BLE_MATCH", "Preferred MAC matched during fallback scan: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }

                if (SensorRegistry.knownDeviceAddresses.contains(address.uppercase())) {
                    SensorLogger.d("BLE_MATCH", "Known MAC matched: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }

                if (name.contains("FORA", ignoreCase = true) ||
                    name.contains("O2") ||
                    name.contains("IR42", ignoreCase = true) ||
                    name.matches(Regex(".*FORA.*", RegexOption.IGNORE_CASE)) ||
                    name.matches(Regex(".*O2.*"))
                ) {
                    SensorLogger.d("BLE_MATCH", "Name matched sensor rules: '$name' ($address)")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }

                val advertisedServices = result.scanRecord?.serviceUuids ?: return
                if (advertisedServices.any { it.uuid == TemperatureConstants.serviceUuid }) {
                    SensorLogger.d("BLE_MATCH", "Temperature service UUID matched: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }
                if (advertisedServices.any { it.uuid == Spo2Constants.serviceUuid }) {
                    SensorLogger.d("BLE_MATCH", "SPO2 service UUID matched: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                    return
                }
                if (advertisedServices.any { it.uuid == BloodPressureConstants.serviceUuid }) {
                    SensorLogger.d("BLE_MATCH", "Blood pressure service UUID matched: $address")
                    stopScan()
                    onTargetFound?.invoke(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                SensorLogger.e(TAG, "Scan failed with error code: $errorCode")
                scanCallback = null
                seenDevices.clear()
                onScanFailed?.invoke(errorCode)
            }
        }

        scanCallback = callback
        @Suppress("UNUSED_VARIABLE")
        val unused = onAdvertisementTemperature
        try {
            scanner?.startScan(null, settings, callback)
            SensorLogger.d(TAG, "Starting continuous unfiltered BLE scan")
        } catch (securityException: SecurityException) {
            SensorLogger.e(TAG, "Missing permission while starting BLE scan", securityException)
            scanCallback = null
        } catch (exception: Exception) {
            SensorLogger.e(TAG, "Unexpected error while starting BLE scan", exception)
            scanCallback = null
        }
    }

    fun stopScan() {
        scanCallback?.let {
            scanner?.stopScan(it)
            scanCallback = null
            SensorLogger.d(TAG, "Scan stopped")
        }
    }

    fun getRemoteDevice(address: String): BluetoothDevice? =
        try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (exception: IllegalArgumentException) {
            SensorLogger.e(TAG, "Invalid Bluetooth device address: $address", exception)
            null
        }

    private companion object {
        const val TAG = "BLE_SCAN"
    }
}
