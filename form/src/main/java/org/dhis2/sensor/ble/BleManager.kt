package org.dhis2.sensor.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dhis2.sensor.config.SensorConfigRepository

private const val TAG = "BleManager"

class BleManager(
    private val context: Context,
    private val sensorConfigRepository: SensorConfigRepository,
) {

    private val bleScanner = BleScanner(context)

    /** Guard against duplicate connections when the scan fires multiple times for the same device. */
    @Volatile private var isConnecting = false

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Emits sensor readings as (key, value) pairs.
     * Keys: "SPO2", "PULSE" for oximeter; characteristic UUID string for thermometer.
     */
    private val _sensorData = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val sensorData: StateFlow<List<Pair<String, String>>> = _sensorData.asStateFlow()

    private val bleDeviceConnector = BleDeviceConnector(
        onConnectionStateChanged = { isConnected ->
            _connectionState.value =
                if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
            if (!isConnected) isConnecting = false
        },
        onReadingsReceived = { readings ->
            Log.d(TAG, "Readings received: $readings")
            _sensorData.value = readings
        },
    )

    fun startScan() {
        _devices.value = emptyList()
        isConnecting = false

        bleScanner.startScan(
            onDeviceFound = { device ->
                val currentList = _devices.value.toMutableList()
                if (!currentList.contains(device)) {
                    currentList.add(device)
                    _devices.value = currentList
                }
            },
            onTargetFound = { device ->
                // Guard: only connect once even if scan fires multiple callbacks
                if (isConnecting) {
                    Log.d(TAG, "Already connecting — ignoring duplicate target: ${device.address}")
                    return@startScan
                }
                isConnecting = true
                val sensorType = KnownDevices.typeFor(device.address)
                Log.d(TAG, "Sensor detected: $sensorType (${device.address}) — connecting")
                connectDevice(device, sensorType)
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
        isConnecting = false
        bleDeviceConnector.disconnect()
    }

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING
    }
}
