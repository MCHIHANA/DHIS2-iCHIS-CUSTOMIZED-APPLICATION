package org.dhis2.sensors.device_manager

import org.dhis2.sensor.config.SensorConfigRepository
import org.dhis2.form.ui.sensor.SensorFieldResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ReconnectManagerTest {
    private val pairedDeviceRepository: PairedDeviceRepository = mock {
        on { getPreferredDevice(DeviceType.TEMPERATURE) } doReturn
            SensorDevice(
                deviceName = "FORA Thermometer",
                macAddress = "C0:26:DA:1B:06:A4",
                deviceType = DeviceType.TEMPERATURE,
                lastConnected = 1234L,
                isPaired = true,
            )
        on { getPreferredDevice(DeviceType.BLOOD_PRESSURE) } doReturn
            SensorDevice(
                deviceName = "FORA BP Monitor",
                macAddress = "C0:26:DA:19:D4:FE",
                deviceType = DeviceType.BLOOD_PRESSURE,
                lastConnected = 5678L,
                isPaired = true,
            )
    }

    private val sensorConfigRepository: SensorConfigRepository = mock()

    @Test
    fun `should return saved device for known temperature field`() {
        val reconnectManager = ReconnectManager(pairedDeviceRepository)

        val device =
            reconnectManager.resolvePreferredDevice(
                fieldUid = "KXNH45ts16S",
                sensorConfigRepository = sensorConfigRepository,
            )

        assertEquals("C0:26:DA:1B:06:A4", device?.macAddress)
    }

    @Test
    fun `should fall back to provided legacy address when no saved device exists`() {
        val reconnectManager = ReconnectManager(mock())

        val macAddress =
            reconnectManager.resolvePreferredMacAddress(
                fieldUid = "unknown-field",
                sensorConfigRepository = sensorConfigRepository,
                fallbackMacAddress = "11:22:33:44:55:66",
            )

        assertEquals("11:22:33:44:55:66", macAddress)
    }

    @Test
    fun `should return null when neither saved nor fallback address exists`() {
        val reconnectManager = ReconnectManager(mock())

        val macAddress =
            reconnectManager.resolvePreferredMacAddress(
                fieldUid = "unknown-field",
                sensorConfigRepository = sensorConfigRepository,
                fallbackMacAddress = null,
            )

        assertNull(macAddress)
    }

    @Test
    fun `should match blood pressure fields to saved BP device`() {
        val reconnectManager = ReconnectManager(pairedDeviceRepository)

        val device =
            reconnectManager.resolvePreferredDevice(
                fieldUid = SensorFieldResolver.SYSTOLIC_UID,
                sensorConfigRepository = sensorConfigRepository,
            )

        assertEquals("C0:26:DA:19:D4:FE", device?.macAddress)
    }
}
