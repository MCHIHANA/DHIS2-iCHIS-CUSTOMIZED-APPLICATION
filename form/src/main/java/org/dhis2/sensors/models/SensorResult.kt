package org.dhis2.sensors.models

import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.core.SensorType

data class SensorResult(
    val sensorType: SensorType,
    val readings: List<SensorReading> = emptyList(),
    val isComplete: Boolean = readings.isNotEmpty(),
)
