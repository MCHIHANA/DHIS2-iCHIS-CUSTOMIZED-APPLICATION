package org.dhis2.sensor.connection

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dhis2.sensor.config.SensorConfigRepository

/**
 * Central connection manager — Bluetooth only.
 * USB and WiFi have been removed; all sensor communication goes through BLE.
 */
class ConnectionManager(
    private val context: Context,
    private val sensorConfigRepository: SensorConfigRepository,
) {

    private val bleManager = BleConnectionManager(context, sensorConfigRepository)

    val devices: StateFlow<List<BluetoothDevice>> = bleManager.devices
    val connectionState: StateFlow<BleConnectionManager.ConnectionState> = bleManager.connectionState
    val sensorData: StateFlow<List<Pair<String, String>>> = bleManager.sensorData

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    /**
     * Start a BLE scan with optional timeout.
     * The scan stops automatically when a known sensor MAC is detected.
     */
    fun connect(
        type: ConnectionType = ConnectionType.BLE,
        scope: CoroutineScope,
        onTimeout: () -> Unit,
    ) {
        _isScanning.value = true
        _connectionError.value = null

        ConnectionTimeoutManager.startTimeout(scope) {
            _isScanning.value = false
            _connectionError.value = "No connections available"
            onTimeout()
        }

        bleManager.startScan()
    }

    /** Legacy connect without explicit timeout. */
    fun connect(type: ConnectionType = ConnectionType.BLE) {
        _isScanning.value = true
        _connectionError.value = null
        bleManager.startScan()
    }

    fun cancelConnection() {
        _isScanning.value = false
        ConnectionTimeoutManager.cancelTimeout()
        bleManager.stopScan()
    }

    fun connectDevice(device: BluetoothDevice) {
        ConnectionTimeoutManager.cancelTimeout()
        _isScanning.value = false
        bleManager.connectDevice(device)
    }

    fun disconnect() {
        ConnectionTimeoutManager.cancelTimeout()
        bleManager.disconnect()
    }
}
