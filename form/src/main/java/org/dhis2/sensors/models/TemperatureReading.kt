package org.dhis2.sensors.models

data class TemperatureReading(
    val value: Double,
    val unit: String = "C",
)
