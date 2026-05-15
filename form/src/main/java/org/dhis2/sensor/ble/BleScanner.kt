package org.dhis2.sensor.ble

import android.bluetooth.BluetoothDevice
import android.content.Context

class BleScanner(context: Context) {
    private val delegate = org.dhis2.sensors.core.BleScanner(context)

    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
        onAdvertisementTemperature: ((Double) -> Unit)? = null,
    ) {
        delegate.startScan(
            onDeviceFound = onDeviceFound,
            onTargetFound = onTargetFound,
            onAdvertisementTemperature = onAdvertisementTemperature,
        )
    }

    fun stopScan() {
        delegate.stopScan()
    }
}
