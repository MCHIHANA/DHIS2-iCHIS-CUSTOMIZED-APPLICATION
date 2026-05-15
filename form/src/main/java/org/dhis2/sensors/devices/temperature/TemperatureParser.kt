package org.dhis2.sensors.devices.temperature

import org.dhis2.sensors.models.TemperatureReading

object TemperatureParser {
    fun parse(data: ByteArray): TemperatureReading? = null

    fun parseRaw(data: ByteArray): Double = 0.0

    fun parseAdvertisementTemperature(bytes: ByteArray): Double? = null
}
