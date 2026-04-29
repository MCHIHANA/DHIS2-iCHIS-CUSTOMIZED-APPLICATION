package org.dhis2.sensor.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dhis2.sensor.config.SensorConfigRepository

private const val TAG = "BleManager"

/** UUID of the standard Temperature Measurement characteristic — used as the key
 *  when emitting advertisement-sourced temperature values so the FormViewModel
 *  observer treats them identically to GATT-sourced values. */
private const val TEMP_CHAR_UUID = "00002A1C-0000-1000-8000-00805F9B34FB"

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
     * Populated by both the advertisement path (FORA IR42) and the GATT path
     * (all other sensors), so FormViewModel needs no changes.
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
     * Starts a continuous BLE scan.
     *
     * **FORA IR42 (advertisement path — no GATT):**
     * When a FORA device is detected the scanner reads the temperature directly
     * from the advertisement packet, emits it to [_sensorData], and stops.
     * No GATT connection is made.
     *
     * **Other known sensors (GATT path):**
     * When a known MAC is detected the scanner stops and [connectDevice] is
     * called to establish a GATT connection and read via notifications.
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
            onAdvertisementTemperature = { temperature ->
                // Temperature read directly from advertisement — no GATT needed.
                // Emit using the standard Temperature Measurement UUID so the
                // FormViewModel observer handles it like any other sensor reading.
                Log.d(TAG, "Advertisement temperature received: $temperature °C")
                _connectionState.value = ConnectionState.CONNECTED
                _sensorData.value = Pair(TEMP_CHAR_UUID, "%.1f".format(temperature))
                // Mark as disconnected after emitting — sensor has powered off
                _connectionState.value = ConnectionState.DISCONNECTED
            },
            onTargetFound = { device ->
                // GATT fallback for non-FORA known-MAC sensors
                Log.d(TAG, "Auto-connecting via GATT to: ${device.address}")
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
