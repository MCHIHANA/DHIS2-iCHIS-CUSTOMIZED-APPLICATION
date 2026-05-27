package org.dhis2.sensors.device_manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class PairedDeviceRepositoryTest {
    private val storageManager: DeviceStorageManager = mock {
        on { loadDevices() } doReturn
            listOf(
                SensorDevice(
                    deviceName = "Older Thermometer",
                    macAddress = "AA:BB:CC:00:00:01",
                    deviceType = DeviceType.TEMPERATURE,
                    lastConnected = 100L,
                    isPaired = true,
                ),
                SensorDevice(
                    deviceName = "Newer Thermometer",
                    macAddress = "AA:BB:CC:00:00:02",
                    deviceType = DeviceType.TEMPERATURE,
                    lastConnected = 200L,
                    isPaired = true,
                ),
            )
    }

    @Test
    fun `should prefer most recently connected device for the same type`() {
        val repository = PairedDeviceRepository(storageManager)

        val preferredDevice = repository.getPreferredDevice(DeviceType.TEMPERATURE)

        assertEquals("AA:BB:CC:00:00:02", preferredDevice?.macAddress)
    }

    @Test
    fun `should persist normalized address when pairing a device`() {
        val repository = PairedDeviceRepository(storageManager)
        val devicesCaptor = argumentCaptor<List<SensorDevice>>()

        repository.pairDevice(
            deviceName = "FORA BP Monitor",
            macAddress = "aa:bb:cc:dd:ee:ff",
            deviceType = DeviceType.BLOOD_PRESSURE,
        )

        verify(storageManager).saveDevices(devicesCaptor.capture())
        assertEquals(
            "AA:BB:CC:DD:EE:FF",
            devicesCaptor.firstValue.first { it.deviceType == DeviceType.BLOOD_PRESSURE }.macAddress,
        )
    }

    @Test
    fun `should remove a saved device by address`() {
        val repository = PairedDeviceRepository(storageManager)

        repository.removeDevice("AA:BB:CC:00:00:02")

        assertNull(repository.findDevice("AA:BB:CC:00:00:02"))
    }
}
