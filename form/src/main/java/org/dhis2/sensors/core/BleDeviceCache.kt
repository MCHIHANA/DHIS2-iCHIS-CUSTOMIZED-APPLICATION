package org.dhis2.sensors.core

import android.content.Context

class BleDeviceCache(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun put(
        sensorType: SensorType,
        address: String,
    ) {
        if (sensorType == SensorType.UNKNOWN || address.isBlank()) {
            return
        }

        sharedPreferences.edit()
            .putString(sensorType.preferenceKey, address.uppercase())
            .apply()
    }

    fun get(sensorType: SensorType): String? =
        sharedPreferences.getString(sensorType.preferenceKey, null)

    private val SensorType.preferenceKey: String
        get() = "sensor_address_${name.lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "ble_device_cache"
    }
}
