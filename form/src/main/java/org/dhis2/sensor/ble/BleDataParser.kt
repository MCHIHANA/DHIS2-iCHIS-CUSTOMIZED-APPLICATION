package org.dhis2.sensor.ble

import org.dhis2.sensors.devices.bloodpressure.BloodPressureParser
import org.dhis2.sensors.devices.temperature.TemperatureParser

object BleDataParser {
    fun parseBloodPressure(data: ByteArray): BloodPressureReading =
        BloodPressureParser.parse(data)

    fun parseHeartRate(data: ByteArray): Int =
        if (data.size < 2) 0 else data[1].toInt() and 0xFF

    fun parseTemperature(data: ByteArray): Float =
        TemperatureParser.parseFloat(data)

    fun parseTemperatureRaw(data: ByteArray): Double =
        TemperatureParser.parseRaw(data)

    fun parseSpO2(data: ByteArray): Int =
        if (data.size < 2) 0 else data[1].toInt() and 0xFF

    fun parseAdvertisementTemperature(bytes: ByteArray): Double? =
        TemperatureParser.parseAdvertisementTemperature(bytes)
}

typealias BloodPressureReading = org.dhis2.sensors.models.BloodPressureReading
