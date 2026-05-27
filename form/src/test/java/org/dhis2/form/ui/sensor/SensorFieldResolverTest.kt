package org.dhis2.form.ui.sensor

import org.dhis2.sensor.ble.KnownDevices
import org.dhis2.sensor.ble.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorFieldResolverTest {

    @Test
    fun `should resolve blood pressure sensor type from systolic uid`() {
        assertEquals(
            SensorType.BLOOD_PRESSURE,
            SensorFieldResolver.resolveSensorType(SensorFieldResolver.SYSTOLIC_UID, sensorConfig = null),
        )
    }

    @Test
    fun `should resolve preferred temperature mac when config is missing`() {
        assertEquals(
            KnownDevices.TEMP_SENSOR,
            SensorFieldResolver.resolvePreferredMacAddress(
                uid = SensorFieldResolver.TEMPERATURE_UID,
                sensorConfig = null,
            ),
        )
    }

    @Test
    fun `should detect completed reading statuses`() {
        assertTrue(SensorFieldResolver.hasCompletedReading(SensorStatusText.dataReceived("37.1")))
        assertFalse(SensorFieldResolver.hasCompletedReading(SensorStatusText.CONNECTED))
    }
}
