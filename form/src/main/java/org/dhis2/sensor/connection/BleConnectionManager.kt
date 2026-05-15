package org.dhis2.sensor.connection

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.dhis2.sensor.ble.BleManager
import org.dhis2.sensor.ble.SensorType
import org.dhis2.sensor.config.SensorConfigRepository

class BleConnectionManager(
    context: Context,
    sensorConfigRepository: SensorConfigRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bleManager = BleManager(context, sensorConfigRepository)

    val devices: StateFlow<List<BluetoothDevice>> = bleManager.devices

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val sensorData: StateFlow<List<Pair<String, String>>> = bleManager.sensorData

    init {
        bleManager.connectionState
            .onEach { state ->
                _connectionState.value = when (state) {
                    BleManager.ConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
                    BleManager.ConnectionState.CONNECTING -> ConnectionState.CONNECTING
                    BleManager.ConnectionState.CONNECTED -> ConnectionState.CONNECTED
                    BleManager.ConnectionState.DISCONNECTING -> ConnectionState.DISCONNECTING
                }
            }
            .launchIn(scope)
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectDevice(
        device: BluetoothDevice,
        sensorType: SensorType = SensorType.UNKNOWN,
    ) {
        bleManager.connectDevice(device, sensorType)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
    }
}
