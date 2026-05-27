package org.dhis2.sensor.ble

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
import org.dhis2.sensor.config.SensorConfigRepository

class BleManager(
    context: Context,
    @Suppress("unused")
    private val sensorConfigRepository: SensorConfigRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val delegate = org.dhis2.sensors.core.BleManager(context)

    val devices: StateFlow<List<BluetoothDevice>> = delegate.devices

    private val _connectionState = MutableStateFlow(delegate.connectionState.value.toLegacy())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val sensorData: StateFlow<List<Pair<String, String>>> = _sensorData.asStateFlow()

    val lastFailure: StateFlow<String?> = delegate.lastFailure

    val currentDeviceAddress: StateFlow<String?> = delegate.currentDeviceAddress
    val currentDeviceName: StateFlow<String?> = delegate.currentDeviceName

    init {
        delegate.connectionState
            .onEach { _connectionState.value = it.toLegacy() }
            .launchIn(scope)

        delegate.sensorData
            .onEach { readings ->
                _sensorData.value = readings.map { it.type to it.value }
            }
            .launchIn(scope)
    }

    fun startScan(
        preferredDeviceAddress: String? = null,
        sensorType: SensorType = SensorType.UNKNOWN,
    ) {
        delegate.startScan(preferredDeviceAddress, sensorType)
    }

    fun stopScan() {
        delegate.stopScan()
    }

    fun connectDevice(
        device: BluetoothDevice,
        sensorType: SensorType = SensorType.UNKNOWN,
    ) {
        delegate.connectDevice(device, sensorType)
    }

    fun disconnect() {
        delegate.disconnect()
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
    }

    private fun org.dhis2.sensors.core.BleManager.ConnectionState.toLegacy(): ConnectionState =
        when (this) {
            org.dhis2.sensors.core.BleManager.ConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
            org.dhis2.sensors.core.BleManager.ConnectionState.CONNECTING -> ConnectionState.CONNECTING
            org.dhis2.sensors.core.BleManager.ConnectionState.CONNECTED -> ConnectionState.CONNECTED
            org.dhis2.sensors.core.BleManager.ConnectionState.DISCONNECTING -> ConnectionState.DISCONNECTING
        }
}
