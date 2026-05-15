package org.dhis2.sensor.ble

import android.bluetooth.BluetoothDevice
import android.content.Context

class BleDeviceConnector(
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onReadingsReceived: (List<Pair<String, String>>) -> Unit,
) {
    private val delegate = org.dhis2.sensors.core.BleConnector(
        onConnectionStateChanged = onConnectionStateChanged,
        onReadingsReceived = { readings ->
            onReadingsReceived(readings.map { it.type to it.value })
        },
    )

    fun connect(
        context: Context,
        device: BluetoothDevice,
        sensorType: SensorType,
    ) {
        delegate.connect(context, device, sensorType)
    }

    fun disconnect() {
        delegate.disconnect()
    }
}
