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

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Emits sensor readings as (characteristicUUID, parsedValue) pairs.
     * Observed by FormViewModel to auto-fill the focused form field.
     */
    private val _sensorData = MutableStateFlow<Pair<String, String>?>(null)
    val sensorData: StateFlow<Pair<String, String>?> = _sensorData.asStateFlow()

    private val bleDeviceConnector = BleDeviceConnector(
        onConnectionStateChanged = { isConnected ->
            _connectionState.value =
                if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        },
        onDataReceived = { uuid, value ->
            _sensorData.value = Pair(uuid, value)
        },
    )

    /**
     * Starts a continuous BLE scan filtered to the Health Thermometer service
     * (UUID 0x1809). When a matching device is detected the scan stops and a
     * GATT connection is established to receive the temperature measurement.
     *
     * Flow:
     *   1. User taps Temperature field → [startScan] is called
     *   2. User turns ON the thermometer
     *   3. Thermometer advertises service UUID 0x1809
     *   4. Scanner detects it → stops scan → connects via GATT
     *   5. GATT discovers services → subscribes to 0x2A1C (INDICATE)
     *   6. Thermometer sends measurement → [_sensorData] is updated
     *   7. FormViewModel observer saves the value to the form field
     */
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
                Log.d(TAG, "Health Thermometer detected — connecting to ${device.address}")
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
