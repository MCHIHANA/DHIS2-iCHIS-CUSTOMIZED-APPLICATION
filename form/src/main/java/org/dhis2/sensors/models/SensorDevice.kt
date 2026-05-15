package org.dhis2.sensors.models

import org.dhis2.sensors.core.SensorType

data class SensorDevice(
    val address: String,
    val name: String?,
    val sensorType: SensorType,
)
