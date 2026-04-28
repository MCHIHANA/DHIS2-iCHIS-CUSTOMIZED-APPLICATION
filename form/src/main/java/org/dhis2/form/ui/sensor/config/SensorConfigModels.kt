package org.dhis2.form.ui.sensor.config

data class SensorConfigResponse(
    val sensors: List<SensorConfig>
)

data class SensorConfig(
    val name: String,
    val dataElement: String?,
    val dataElements: BloodPressureElements?,
    val unit: String?,
    val serviceUUID: String?,
    val characteristicUUID: String?,
    val sensorRequired: Boolean,
    val manualAllowed: Boolean
)

data class BloodPressureElements(
    val systolic: String,
    val diastolic: String
)
