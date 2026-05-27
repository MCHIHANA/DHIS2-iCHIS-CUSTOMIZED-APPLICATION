package org.dhis2.sensors.device_manager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DeviceStorageManager(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val deviceListType = object : TypeToken<List<SensorDevice>>() {}.type

    fun loadDevices(): List<SensorDevice> {
        val json = sharedPreferences.getString(KEY_PAIRED_DEVICES, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<SensorDevice>>(json, deviceListType).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun saveDevices(devices: List<SensorDevice>) {
        sharedPreferences.edit()
            .putString(KEY_PAIRED_DEVICES, gson.toJson(devices))
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "device_manager_preferences"
        const val KEY_PAIRED_DEVICES = "paired_devices"
    }
}
