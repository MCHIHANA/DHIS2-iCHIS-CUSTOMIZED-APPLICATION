package org.dhis2.sensor.config

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
    private val sharedPreferences = context.getSharedPreferences("sensor_config_prefs", Context.MODE_PRIVATE)

    suspend fun fetchSensorConfig() {
        try {
            val response = api.getSensorConfig()
            sensorConfigFlow.value = response.sensors
            cacheConfig(response)
            Log.d("SensorConfig", "Loaded sensors: ${response.sensors.size}")
            response.sensors.forEach { sensor ->
                Log.d("SensorConfig", sensor.name)
                if (sensor.serviceUUID != null) {
                    Log.d("SensorConfig", sensor.serviceUUID)
                }
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

    fun findConfigByName(name: String): SensorConfig? {
        return sensorConfigFlow.value.find { 
            it.name.equals(name, ignoreCase = true) 
        }
    }

    fun getConfigByDataElement(uid: String): SensorConfig? {
        return sensorConfigFlow.value.find { config ->
            // Check legacy single-value
            if (config.dataElement == uid) return@find true
            
            // Check legacy dual-value (systolic/diastolic)
            if (config.dataElements?.systolic == uid || config.dataElements?.diastolic == uid) {
                return@find true
            }
            
            // Check new multi-measurement structure
            config.measurements?.values?.any { it.dataElement == uid } == true
        }
    }
    
    /**
     * Returns all data element UIDs for a given sensor configuration.
     * Supports both legacy and new multi-measurement structures.
     */
    fun getDataElementsForConfig(config: SensorConfig): List<String> {
        val elements = mutableListOf<String>()
        
        // New multi-measurement structure
        config.measurements?.values?.forEach { measurement ->
            elements.add(measurement.dataElement)
        }
        
        // Legacy structures
        config.dataElement?.let { elements.add(it) }
        config.dataElements?.let {
            elements.add(it.systolic)
            elements.add(it.diastolic)
        }
        
        return elements
    }
    
    /**
     * Maps a measurement key (e.g., "systolic", "pulse") to its data element UID.
     * Returns null if the measurement is not configured.
     */
    fun getDataElementForMeasurement(config: SensorConfig, measurementKey: String): String? {
        return config.measurements?.get(measurementKey)?.dataElement
    }
}
