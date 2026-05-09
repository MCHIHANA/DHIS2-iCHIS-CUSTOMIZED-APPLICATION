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
        Log.d("SensorConfig", "=== fetchSensorConfig() called ===")
        try {
            Log.d("SensorConfig", "Calling API to fetch sensor config...")
            val response = api.getSensorConfig()
            Log.d("SensorConfig", "API call successful! Received ${response.sensors.size} sensors")
            sensorConfigFlow.value = response.sensors
            cacheConfig(response)
            Log.d("SensorConfig", "✓ Loaded sensors: ${response.sensors.size}")
            response.sensors.forEach { sensor ->
                Log.d("SensorConfig", "  - ${sensor.name}")
                if (sensor.serviceUUID != null) {
                    Log.d("SensorConfig", "    Service UUID: ${sensor.serviceUUID}")
                }
                if (sensor.measurements != null) {
                    Log.d("SensorConfig", "    Measurements: ${sensor.measurements.keys}")
                }
            }
            Log.d("SensorConfig", "=== Config loaded successfully ===")
        } catch (e: Exception) {
            Log.e("SensorConfig", "❌ Fetch failed! Error: ${e.message}", e)
            Log.e("SensorConfig", "Exception type: ${e.javaClass.simpleName}")
            Log.d("SensorConfig", "Attempting to load from cache...")
            loadFromCache()
            if (sensorConfigFlow.value.isEmpty()) {
                Log.e("SensorConfig", "❌ Cache is also empty! No sensor config available.")
            } else {
                Log.d("SensorConfig", "✓ Loaded ${sensorConfigFlow.value.size} sensors from cache")
            }
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
        Log.d("SensorConfig", "Loading from SharedPreferences cache...")
        val json = sharedPreferences.getString("sensor_config", null)
        if (json == null) {
            Log.w("SensorConfig", "No cached config found in SharedPreferences")
            return
        }
        Log.d("SensorConfig", "Found cached config (${json.length} chars)")
        json?.let {
            try {
                val cached = Gson().fromJson(it, SensorConfigResponse::class.java)
                sensorConfigFlow.value = cached.sensors
                Log.d("SensorConfig", "✓ Successfully loaded ${cached.sensors.size} sensors from cache")
            } catch (e: Exception) {
                Log.e("SensorConfig", "❌ Cache parsing failed", e)
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
