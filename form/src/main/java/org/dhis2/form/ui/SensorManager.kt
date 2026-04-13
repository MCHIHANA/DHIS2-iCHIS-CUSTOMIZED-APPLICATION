package org.dhis2.form.ui

import kotlin.random.Random

class SensorManager {
    fun readSensor(type: SensorType): String {
        return when (type) {
            SensorType.TEMPERATURE -> String.format("%.1f", Random.nextDouble(36.0, 39.0))
            SensorType.WEIGHT -> String.format("%.1f", Random.nextDouble(50.0, 100.0))
            else -> ""
        }
    }
}
