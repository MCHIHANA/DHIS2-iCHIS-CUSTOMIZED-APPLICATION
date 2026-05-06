package org.dhis2.sensor.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Emits one or more sensor readings as a list of (characteristicUUID, parsedValue) pairs.
     *
     * Single-value sensors (thermometer) emit a list with one entry.
     * Multi-value sensors (oximeter) emit a list with two entries:
     *   - ("SPO2",  "98")   — SpO2 percentage
     *   - ("PULSE", "72")   — Pulse rate in bpm
     *
     * FormViewModel observes this and routes each reading to the correct field.
     */
    private val _sensorData = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val sensorData: StateFlow<List<Pair<String, String>>> = _sensorData.asStateFlow()

    // Single-value compat property used by legacy observers
    private val _singleSensorData = MutableStateFlow<Pair<String, String>?>(null)

    private val bleDeviceConnector = BleDeviceConnector(
        onConnectionStateChanged = { isConnected ->
            _connectionState.value =
                if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        },
        onReadingsReceived = { readings ->
            _sensorData.value = readings
            // Also populate single-value flow for backward compat
            readings.firstOrNull()?.let { _singleSensorData.value = it }
        },
    )

    /**
     * Starts a continuous BLE scan. When a known device is detected the scan
     * stops and a GATT connection is established on the main thread.
     *
     * The [BleDeviceConnector] is told the [SensorType] so it can subscribe to
     * the correct GATT characteristics for that device.
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
                val sensorType = KnownDevices.typeFor(device.address)
                Log.d(TAG, "Sensor detected: $sensorType (${device.address}) — connecting")
                // connectGatt() MUST be called on the main thread
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
