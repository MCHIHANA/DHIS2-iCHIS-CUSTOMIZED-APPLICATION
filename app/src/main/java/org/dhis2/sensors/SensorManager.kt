package org.dhis2.sensors

class SensorManager {
    fun readSensor(type: SensorType): String {
        return when (type) {
            SensorType.TEMPERATURE -> "36.7"
            SensorType.WEIGHT -> "65"
            else -> ""
        }
    }
}
