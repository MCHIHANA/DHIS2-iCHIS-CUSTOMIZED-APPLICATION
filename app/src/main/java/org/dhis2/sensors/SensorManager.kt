package org.dhis2.sensors

import kotlin.random.Random


class SensorManager {
    fun readSensor(type: SensorType): String {
        return when (type) {
            SensorType.TEMPERATURE -> "36.7"
            SensorType.WEIGHT -> "65"
            SensorType.HEART_RATE -> "72"
            SensorType.BLOOD_PRESSURE -> "120/80"

            else -> ""
        }
    }
}
