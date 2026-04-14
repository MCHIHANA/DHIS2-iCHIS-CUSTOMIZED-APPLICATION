package org.dhis2.sensors

import kotlin.random.Random


/**
 * Manager class for simulating medical sensor readings.
 */
class SensorManager {
    /**
     * Reads a simulated value for the given sensor [type].
     *
     * @param type The type of sensor to read.
     * @return A string representation of the simulated reading.
     */
    fun readSensor(type: SensorType): String {
        return when (type) {
            SensorType.TEMPERATURE -> String.format("%.1f", Random.nextDouble(36.0, 38.0))
            SensorType.WEIGHT -> Random.nextInt(50, 90).toString()

            SensorType.HEART_RATE -> Random.nextInt(60, 100).toString()

            SensorType.BLOOD_PRESSURE -> "120/80"

            else -> ""
        }
    }

    /**
     * Validates a sensor reading value.
     *
     * @param type The type of sensor.
     * @param value The value to validate.
     * @return true if valid, false otherwise.
     */
    fun validateSensorValue(type: SensorType, value: String?): Boolean {
        if (value.isNullOrEmpty()) return true // Allow empty values

        return when (type) {
            SensorType.TEMPERATURE -> {
                val temp = value.toDoubleOrNull()
                temp != null && temp in 30.0..45.0
            }
            SensorType.WEIGHT -> {
                val weight = value.toIntOrNull()
                weight != null && weight in 1..300
            }
            SensorType.HEART_RATE -> {
                val heartRate = value.toIntOrNull()
                heartRate != null && heartRate in 30..200
            }
            SensorType.BLOOD_PRESSURE -> {
                value.matches(Regex("\\d+/\\d+")) // Format like 120/80
            }
            else -> true
        }
    }

    /**
     * Logs a sensor reading.
     *
     * @param type The type of sensor.
     * @param value The value recorded.
     */
    fun logSensorReading(type: SensorType, value: String) {
        val logMessage = when (type) {
            SensorType.TEMPERATURE -> "Temperature recorded: $value"
            SensorType.WEIGHT -> "Weight recorded: $value"
            SensorType.HEART_RATE -> "Heart Rate recorded: $value"
            SensorType.BLOOD_PRESSURE -> "Blood Pressure recorded: $value"
            else -> "$type recorded: $value"
        }
        println(logMessage) // Or use Timber.d(logMessage)
    }
}
