package org.dhis2.sensor.config

import com.google.gson.annotations.SerializedName

data class SensorConfigResponse(
    @SerializedName("sensors")
    val sensors: List<SensorConfig>,
)

data class SensorConfig(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("macAddress")
    val macAddress: String? = null,
    @SerializedName("measurements")
    val measurements: Map<String, MeasurementConfig>? = null,
    @SerializedName("dataElement")
    val dataElement: String? = null,
    @SerializedName("dataElements")
    val dataElements: BloodPressureElements? = null,
    @SerializedName("unit")
    val unit: String? = null,
    @SerializedName("serviceUUID")
    val serviceUUID: String? = null,
    @SerializedName("characteristicUUID")
    val characteristicUUID: String? = null,
    @SerializedName("sensorRequired")
    val sensorRequired: Boolean = false,
    @SerializedName("manualAllowed")
    val manualAllowed: Boolean = true,
)

data class MeasurementConfig(
    @SerializedName("dataElement")
    val dataElement: String,
    @SerializedName("unit")
    val unit: String,
)

data class BloodPressureElements(
    @SerializedName("systolic")
    val systolic: String,
    @SerializedName("diastolic")
    val diastolic: String,
)
