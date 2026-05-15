package org.dhis2.sensors.core

import android.bluetooth.BluetoothDevice
import android.content.Context

class BleConnector(
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onReadingsReceived: (List<SensorReading>) -> Unit,
) {
    fun connect(
        context: Context,
        device: BluetoothDevice,
        sensorType: SensorType,
    ) {
        @Suppress("UNUSED_VARIABLE")
        val unused = Triple(context, device, sensorType)
        onConnectionStateChanged(false)
        onReadingsReceived(emptyList())
    }

    fun disconnect() = onConnectionStateChanged(false)
}
