package org.dhis2.sensors.devices.bloodpressure

import org.dhis2.sensors.models.BloodPressureReading

object BloodPressureParser {
    fun parse(data: ByteArray): BloodPressureReading =
        BloodPressureReading(
            systolic = 0f,
            diastolic = 0f,
            map = 0f,
            pulseRate = null,
        )
}
