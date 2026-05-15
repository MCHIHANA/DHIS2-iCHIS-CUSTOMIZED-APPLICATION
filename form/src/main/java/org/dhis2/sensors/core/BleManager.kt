package org.dhis2.sensors.core

import android.bluetooth.BluetoothDevice
import android.content.Context
import org.dhis2.sensors.utils.SensorLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleManager(
    private val context: Context,
) {
    private val bleScanner = BleScanner(context)
    private val bleConnector = BleConnector(
        onConnectionStateChanged = { isConnected ->
            _connectionState.value = if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
            if (!isConnected) {
                isConnecting = false
            }
            SensorLogger.d(TAG, "Connection state changed: ${_connectionState.value}")
        },
        onReadingsReceived = { readings ->
            SensorLogger.d(TAG, "Readings received from device: ${readings.size}")
            _sensorData.value = readings
        },
    )

    @Volatile
    private var isConnecting = false

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<List<SensorReading>>(emptyList())
    val sensorData: StateFlow<List<SensorReading>> = _sensorData.asStateFlow()

    fun startScan() {
        _devices.value = emptyList()
        isConnecting = false
        SensorLogger.d(TAG, "Starting BLE scan")
        bleScanner.startScan(
            onDeviceFound = { device ->
                val currentDevices = _devices.value.toMutableList()
                if (!currentDevices.contains(device)) {
                    currentDevices.add(device)
                    _devices.value = currentDevices
                    SensorLogger.d(TAG, "Device found: ${device.name ?: "Unknown"} (${device.address})")
                }
            },
            onTargetFound = { device ->
                if (isConnecting) {
                    SensorLogger.d(TAG, "Ignoring duplicate target while connecting: ${device.address}")
                    return@startScan
                }
                isConnecting = true
                val sensorType = SensorRegistry.getSensorType(device.address)
                SensorLogger.d(TAG, "Target sensor found: $sensorType (${device.address})")
                connectDevice(device, sensorType)
            },
        )
    }

    fun stopScan() {
        bleScanner.stopScan()
    }

    fun connectDevice(
        device: BluetoothDevice,
        sensorType: SensorType = SensorType.UNKNOWN,
    ) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING
        bleConnector.connect(context, device, sensorType)
    }

    fun disconnect() {
        isConnecting = false
        bleConnector.disconnect()
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
    }

    private companion object {
        const val TAG = "BleManager"
    }
}
