package org.dhis2.sensor.config

import android.util.Log
import org.hisp.dhis.android.core.D2
import com.google.gson.Gson

class SensorConfigApi(private val d2: D2) {

    suspend fun getSensorConfig(): SensorConfigResponse {
        Log.d("SensorConfigApi", "=== getSensorConfig() called ===")
        Log.d("SensorConfigApi", "Querying DataStore: namespace='sensor-config', key='vital-sensor-mapping'")
        
        try {
            val dataStoreEntries = d2.dataStoreModule().dataStore()
                .byNamespace().eq("sensor-config")
                .byKey().eq("vital-sensor-mapping")
                .blockingGet()
            
            Log.d("SensorConfigApi", "DataStore query returned ${dataStoreEntries.size} entries")
            
            val firstEntry = dataStoreEntries.firstOrNull()
            if (firstEntry == null) {
                Log.e("SensorConfigApi", "❌ No DataStore entry found!")
                Log.e("SensorConfigApi", "Make sure you have created the DataStore entry:")
                Log.e("SensorConfigApi", "  Namespace: sensor-config")
                Log.e("SensorConfigApi", "  Key: vital-sensor-mapping")
                throw Exception("Config not found in DataStore")
            }
            
            val json = firstEntry.value()
            Log.d("SensorConfigApi", "✓ Found DataStore entry (${json?.length ?: 0} chars)")
            
            if (json == null) {
                Log.e("SensorConfigApi", "❌ DataStore entry value is null!")
                throw Exception("Config not found in DataStore")
            }
            
            Log.d("SensorConfigApi", "Parsing JSON...")
            val response = Gson().fromJson(json, SensorConfigResponse::class.java)
            Log.d("SensorConfigApi", "✓ Successfully parsed ${response.sensors.size} sensors")
            
            return response
        } catch (e: Exception) {
            Log.e("SensorConfigApi", "❌ Error fetching sensor config", e)
            throw e
        }
    }
}
