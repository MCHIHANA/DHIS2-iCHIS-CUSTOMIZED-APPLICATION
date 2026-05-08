package org.dhis2.sensor.connection

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dhis2.sensor.ble.BleDeviceConnector
import org.dhis2.sensor.ble.BleScanner
import org.dhis2.sensor.ble.KnownDevices
import org.dhis2.sensor.ble.SensorType
import org.dhis2.sensor.config.SensorConfigRepository

class BleConnectionManager(
    private val context: Context,
    private val sensorConfigRepository: SensorConfigRepository,
) {

    private val bleScanner = BleScanner(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Multi-value readings: list of (key, value) pairs. */
    private val _sensorData = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val sensorData: StateFlow<List<Pair<String, String>>> = _sensorData.asStateFlow()

    private val bleDeviceConnector = BleDeviceConnector(
        onConnectionStateChanged = { isConnected ->
            _connectionState.value =
                if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        },
        onReadingsReceived = { readings ->
            _sensorData.value = readings
        },
    )

    fun startScan() {
        _devices.value = emptyList()
        bleScanner.startScan(
            onDeviceFound = { device ->
                val currentList = _devices.value.toMutableList()
                if (!currentList.contains(device)) {
                    currentList.add(device)
                    _devices.value = currentList
                }
            },
            onTargetFound = { device ->
                val sensorType = KnownDevices.typeFor(device.address)
                // connectGatt must run on the main thread
                mainHandler.post { connectDevice(device, sensorType) }
            },
        )
    }

    fun stopScan() {
        bleScanner.stopScan()
    }

    fun connectDevice(device: BluetoothDevice, sensorType: SensorType = SensorType.UNKNOWN) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING
        bleDeviceConnector.connect(context, device, sensorType)
    }

    fun disconnect() {
        bleDeviceConnector.disconnect()
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING
    }
}
