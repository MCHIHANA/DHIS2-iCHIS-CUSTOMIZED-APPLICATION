package org.dhis2.sensors.devices.bloodpressure

import org.dhis2.sensors.models.BloodPressureReading
import org.dhis2.sensors.utils.ByteUtils
import org.dhis2.sensors.utils.SensorLogger

private const val TAG = "BLE_BP"

object BloodPressureParser {
    fun parse(data: ByteArray): BloodPressureReading {
        SensorLogger.d(TAG, "Blood pressure packet (${data.size} bytes): ${ByteUtils.formatBytes(data)}")
        if (data.size < 7) {
            SensorLogger.w(TAG, "Packet too short for blood pressure parsing")
            return BloodPressureReading(0f, 0f, 0f, null)
        }

        val flags = data[0].toInt() and 0xFF
        val unitsKpa = (flags and 0x01) != 0
        val timestampPresent = (flags and 0x02) != 0
        val pulseRatePresent = (flags and 0x04) != 0
        val userIdPresent = (flags and 0x08) != 0
        val statusPresent = (flags and 0x10) != 0

        val systolic = ByteUtils.parseSFloat(data, 1)
        val diastolic = ByteUtils.parseSFloat(data, 3)
        val map = ByteUtils.parseSFloat(data, 5)

        var offset = 7
        var pulseRate: Float? = null

        if (timestampPresent && data.size >= offset + 7) {
            offset += 7
        }

        if (pulseRatePresent && data.size >= offset + 2) {
            pulseRate = ByteUtils.parseSFloat(data, offset)
            offset += 2
        }

        if (userIdPresent && data.size >= offset + 1) {
            offset += 1
        }

        if (statusPresent && data.size >= offset + 2) {
            offset += 2
        }

        val scale = if (unitsKpa) 7.50062f else 1f
        val reading = BloodPressureReading(
            systolic = systolic * scale,
            diastolic = diastolic * scale,
            map = map * scale,
            pulseRate = pulseRate,
        )
        SensorLogger.d(TAG, "Blood pressure parsed: $reading")
        return reading
    }
}
