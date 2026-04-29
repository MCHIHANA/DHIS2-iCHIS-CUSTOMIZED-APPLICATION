package org.dhis2.sensor.config

import com.google.gson.annotations.SerializedName

data class SensorConfigResponse(
    @SerializedName("sensors")
    val sensors: List<SensorConfig>
)

data class SensorConfig(
    @SerializedName("name")
    val name: String,
    @SerializedName("dataElement")
    val dataElement: String?,
    @SerializedName("dataElements")
    val dataElements: BloodPressureElements?,
    @SerializedName("unit")
    val unit: String?,
    @SerializedName("serviceUUID")
    val serviceUUID: String?,
    @SerializedName("characteristicUUID")
    val characteristicUUID: String?,
    @SerializedName("sensorRequired")
    val sensorRequired: Boolean = false,
    @SerializedName("manualAllowed")
    val manualAllowed: Boolean = true
)

data class BloodPressureElements(
    @SerializedName("systolic")
    val systolic: String,
    @SerializedName("diastolic")
    val diastolic: String
)
