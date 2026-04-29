package org.dhis2.sensor.config

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager

object SensorAvailabilityManager {

    fun isBleSupported(context: Context): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false
        }
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager?.adapter
        return adapter != null
    }
}
