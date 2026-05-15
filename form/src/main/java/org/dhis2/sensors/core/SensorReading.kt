package org.dhis2.sensors.core

data class SensorReading(
    val type: String,
    val value: String,
    val unit: String = "",
)
