package org.dhis2.sensors.devices.spo2

import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.utils.ValidationUtils

object Spo2Parser {
    fun parse(data: ByteArray): List<SensorReading> {
        if (data.size < 3) {
            return emptyList()
        }

        val spo2Raw = data[1].toInt() and 0xFF
        if (spo2Raw == 0x7F || spo2Raw == 0xFF || spo2Raw == 0 || !ValidationUtils.isValidSpo2(spo2Raw)) {
            return emptyList()
        }

        val pulseLow = data[2].toInt() and 0xFF
        if (pulseLow == 0xFF) {
            return emptyList()
        }
        val pulseHigh = if (data.size > 3) data[3].toInt() and 0xFF else 0
        var pulse = pulseLow or (pulseHigh shl 8)
        if (pulse !in 20..300) {
            pulse = pulseLow
        }
        if (!ValidationUtils.isValidPulse(pulse.toFloat())) {
            return emptyList()
        }

        return listOf(
            SensorReading(type = "SPO2", value = spo2Raw.toString(), unit = "%"),
            SensorReading(type = "PULSE", value = pulse.toString(), unit = "bpm"),
        )
    }
}
