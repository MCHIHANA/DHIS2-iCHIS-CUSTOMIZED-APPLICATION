package org.dhis2.sensors.utils

object ValidationUtils {
    fun isBodyTemperature(value: Double): Boolean = value in 30.0..45.0

    fun isValidBloodPressure(
        systolic: Float,
        diastolic: Float,
    ): Boolean = systolic in 50f..250f && diastolic in 30f..150f

    fun isValidPulse(value: Float): Boolean = value in 30f..250f

    fun isValidSpo2(value: Int): Boolean = value in 50..100
}
