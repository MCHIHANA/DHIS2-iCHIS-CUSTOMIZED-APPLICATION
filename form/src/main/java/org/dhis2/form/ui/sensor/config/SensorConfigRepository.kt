package org.dhis2.form.ui.sensor.config

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SensorConfigRepository(
    private val context: Context,
    private val api: SensorConfigApi
) {

    private val sensorConfigFlow = MutableStateFlow<List<SensorConfig>>(emptyList())
    private val sharedPreferences = context.getSharedPreferences("sensor_settings", Context.MODE_PRIVATE)

    suspend fun fetchSensorConfig() {
        try {
            val response = api.getSensorConfig()
            sensorConfigFlow.value = response.sensors
            cacheConfig(response)
            Log.d("SensorConfig", "Loaded sensors: ${response.sensors.size}")
            response.sensors.forEach {
                Log.d("SensorConfig", "Sensor: ${it.name}, serviceUUID: ${it.serviceUUID}")
            }
        } catch (e: Exception) {
            Log.e("SensorConfig", "Fetch failed — loading cache", e)
            loadFromCache()
        }
    }

    fun getSensorConfigs(): StateFlow<List<SensorConfig>> {
        return sensorConfigFlow.asStateFlow()
    }

    private fun cacheConfig(response: SensorConfigResponse) {
        val json = Gson().toJson(response)
        sharedPreferences.edit().putString("sensor_config", json).apply()
    }

    private fun loadFromCache() {
        val json = sharedPreferences.getString("sensor_config", null)
        json?.let {
            try {
                val cached = Gson().fromJson(it, SensorConfigResponse::class.java)
                sensorConfigFlow.value = cached.sensors
            } catch (e: Exception) {
                Log.e("SensorConfig", "Cache parsing failed", e)
            }
        }
    }

    fun findConfigByServiceUUID(uuid: UUID): SensorConfig? {
        val uuidString = uuid.toString().lowercase()
        return sensorConfigFlow.value.find { 
            it.serviceUUID?.lowercase() == uuidString 
        }
    }

    fun getConfigForField(label: String): SensorConfig? {
        return sensorConfigFlow.value.find { 
            label.contains(it.name, ignoreCase = true) 
        }
    }
}
