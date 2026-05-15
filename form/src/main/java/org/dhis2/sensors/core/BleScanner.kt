package org.dhis2.sensors.core

import android.bluetooth.BluetoothDevice
import android.content.Context

class BleScanner(
    @Suppress("unused")
    private val context: Context,
) {
    fun startScan(
        onDeviceFound: (BluetoothDevice) -> Unit,
        onTargetFound: ((BluetoothDevice) -> Unit)? = null,
        onAdvertisementTemperature: ((Double) -> Unit)? = null,
    ) {
        @Suppress("UNUSED_VARIABLE")
        val unused = Triple(onDeviceFound, onTargetFound, onAdvertisementTemperature)
    }

    fun stopScan() = Unit
}
