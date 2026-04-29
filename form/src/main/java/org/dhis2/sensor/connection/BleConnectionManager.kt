package org.dhis2.sensor.connection

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dhis2.sensor.ble.BleDeviceConnector
import org.dhis2.sensor.ble.BleScanner
import org.dhis2.sensor.config.SensorConfigRepository

class BleConnectionManager(
    private val context: Context,
    private val sensorConfigRepository: SensorConfigRepository
) {

    private val bleScanner = BleScanner(context)
    
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<Pair<String, String>?>(null) // UUID, Value
    val sensorData: StateFlow<Pair<String, String>?> = _sensorData.asStateFlow()

    private val bleDeviceConnector = BleDeviceConnector(
        onConnectionStateChanged = { isConnected ->
            _connectionState.value = if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        },
        onDataReceived = { uuid, value ->
            _sensorData.value = Pair(uuid, value)
        }
    )

    fun startScan() {
        _devices.value = emptyList() // clear previous scan
        bleScanner.startScan(
            onDeviceFound = { device ->
                val currentList = _devices.value.toMutableList()
                if (!currentList.contains(device)) {
                    currentList.add(device)
                    _devices.value = currentList
                }
            },
            onTargetFound = { device ->
                connectDevice(device)
            },
        )
    }

    fun stopScan() {
        bleScanner.stopScan()
    }

    fun connectDevice(device: BluetoothDevice) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING
        bleDeviceConnector.connect(context, device)
    }

    fun disconnect() {
        bleDeviceConnector.disconnect()
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING
    }
}
