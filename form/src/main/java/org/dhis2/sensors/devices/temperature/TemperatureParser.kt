package org.dhis2.sensors.devices.temperature

import org.dhis2.sensors.models.TemperatureReading
import org.dhis2.sensors.utils.ByteUtils
import org.dhis2.sensors.utils.SensorLogger
import org.dhis2.sensors.utils.ValidationUtils

private const val TAG = "BLE_PARSER"

object TemperatureParser {
    fun parse(data: ByteArray): TemperatureReading {
        val raw = parseRaw(data)
        val value = if (ValidationUtils.isBodyTemperature(raw)) {
            raw
        } else {
            ByteUtils.parseFloat11073(data).toDouble()
        }
        SensorLogger.d(TAG, "Temperature parsed: $value C")
        return TemperatureReading(value = value)
    }

    fun parseFloat(data: ByteArray): Float = ByteUtils.parseFloat11073(data)

    fun parseRaw(data: ByteArray): Double {
        if (data.size < 3) {
            return 0.0
        }
        val tempRaw = ByteUtils.readUInt16LittleEndian(data, 1)
        val temperature = tempRaw / 100.0
        SensorLogger.d(TAG, "Raw temperature parsed: $temperature C")
        return temperature
    }

    fun parseAdvertisementTemperature(bytes: ByteArray): Double? {
        SensorLogger.d("BLE_RAW", "Manufacturer data (${bytes.size} bytes): ${ByteUtils.formatBytes(bytes)}")
        if (bytes.size < 4) {
            return null
        }

        return tryLayout(bytes, 2, 10.0)
            ?: tryLayout(bytes, 1, 10.0)
            ?: if (bytes.size >= 5) tryLayout(bytes, 3, 100.0) else null
    }

    private fun tryLayout(
        bytes: ByteArray,
        byteIndex: Int,
        divisor: Double,
    ): Double? {
        if (bytes.size < byteIndex + 2) {
            return null
        }
        val raw = ByteUtils.readUInt16LittleEndian(bytes, byteIndex)
        val temperature = raw / divisor
        SensorLogger.d(TAG, "Advertisement layout at $byteIndex -> $temperature C")
        return temperature.takeIf(ValidationUtils::isBodyTemperature)
    }
}
