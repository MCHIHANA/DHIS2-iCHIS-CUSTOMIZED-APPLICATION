package org.dhis2.sensors.core

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleManager(
    @Suppress("unused")
    private val context: Context,
) {
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<List<SensorReading>>(emptyList())
    val sensorData: StateFlow<List<SensorReading>> = _sensorData.asStateFlow()

    fun startScan() = Unit

    fun stopScan() = Unit

    fun connectDevice(
        device: BluetoothDevice,
        sensorType: SensorType = SensorType.UNKNOWN,
    ) {
        @Suppress("UNUSED_VARIABLE")
        val unused = device to sensorType
    }

    fun disconnect() = Unit

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
    }
}
