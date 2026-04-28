package org.dhis2.form.ui.sensor.config

import org.hisp.dhis.android.core.D2

class SensorConfigApi(private val d2: D2) {

    suspend fun getSensorConfig(): SensorConfigResponse {
        val json = d2.dataStoreModule().dataStore()
            .byNamespace().eq("sensor-config")
            .byKey().eq("vital-sensor-mapping")
            .blockingGet()
            .firstOrNull()?.value() ?: throw Exception("Config not found")

        return com.google.gson.Gson().fromJson(json, SensorConfigResponse::class.java)
    }
}
