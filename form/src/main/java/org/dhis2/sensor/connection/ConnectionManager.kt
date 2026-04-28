package org.dhis2.sensor.connection

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dhis2.sensor.config.SensorConfigRepository

/**
 * Central connection manager for all sensor types (BLE, USB, WiFi).
 * Handles connection timeouts and provides unified interface.
 */
class ConnectionManager(
    private val context: Context,
    private val sensorConfigRepository: SensorConfigRepository
) {

    private val bleManager = BleConnectionManager(context, sensorConfigRepository)
    private val usbManager = UsbConnectionManager(context)
    private val wifiManager = WifiConnectionManager(context)

    // Connection state
    private val _currentConnectionType = MutableStateFlow<ConnectionType?>(null)
    val currentConnectionType: StateFlow<ConnectionType?> = _currentConnectionType.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    // Using BLE state as the primary source for now
    val devices: StateFlow<List<BluetoothDevice>> = bleManager.devices
    val connectionState: StateFlow<BleConnectionManager.ConnectionState> = bleManager.connectionState
    val sensorData: StateFlow<Pair<String, String>?> = bleManager.sensorData

    init {
        // Set up USB callbacks
        usbManager.setCallbacks(
            onDeviceFound = { device ->
                _isScanning.value = false
                _connectionError.value = null
                ConnectionTimeoutManager.cancelTimeout()
            },
            onNoDeviceFound = {
                _isScanning.value = false
                _connectionError.value = "No USB devices found"
            }
        )

        // Set up WiFi callbacks
        wifiManager.setCallbacks(
            onDeviceFound = { device ->
                _isScanning.value = false
                _connectionError.value = null
                ConnectionTimeoutManager.cancelTimeout()
            },
            onNoDeviceFound = {
                _isScanning.value = false
                _connectionError.value = "No WiFi devices found"
            }
        )
    }

    /**
     * Start connection with specified type and timeout.
     */
    fun connect(
        type: ConnectionType,
        scope: CoroutineScope,
        onTimeout: () -> Unit
    ) {
        _currentConnectionType.value = type
        _isScanning.value = true
        _connectionError.value = null

        // Start timeout
        ConnectionTimeoutManager.startTimeout(scope) {
            _isScanning.value = false
            _connectionError.value = "No connections available"
            onTimeout()
        }

        when(type) {
            ConnectionType.BLE -> bleManager.startScan()
            ConnectionType.USB -> usbManager.connectUsb()
            ConnectionType.WIFI -> wifiManager.connectWifi()
        }
    }

    /**
     * Legacy connect method without timeout.
     */
    fun connect(type: ConnectionType) {
        _currentConnectionType.value = type
        _isScanning.value = true
        _connectionError.value = null

        when(type) {
            ConnectionType.BLE -> bleManager.startScan()
            ConnectionType.USB -> usbManager.connectUsb()
            ConnectionType.WIFI -> wifiManager.connectWifi()
        }
    }

    /**
     * Cancel any active connection attempt.
     */
    fun cancelConnection() {
        _isScanning.value = false
        ConnectionTimeoutManager.cancelTimeout()
        bleManager.stopScan()
    }

    fun connectDevice(device: BluetoothDevice) {
        ConnectionTimeoutManager.cancelTimeout()
        bleManager.connectDevice(device)
    }

    fun disconnect() {
        ConnectionTimeoutManager.cancelTimeout()
        bleManager.disconnect()
        _currentConnectionType.value = null
    }

    /**
     * Get available devices for specified connection type.
     */
    fun getAvailableDevices(type: ConnectionType): List<Any> {
        return when(type) {
            ConnectionType.BLE -> bleManager.devices.value
            ConnectionType.USB -> usbManager.scanForDevices()
            ConnectionType.WIFI -> wifiManager.scanForDevices()
        }
    }
}
