package org.dhis2.sensors.models

data class BloodPressureReading(
    val systolic: Float,
    val diastolic: Float,
    val map: Float,
    val pulseRate: Float?,
)
