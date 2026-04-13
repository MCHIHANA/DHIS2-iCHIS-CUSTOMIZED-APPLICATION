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
}
