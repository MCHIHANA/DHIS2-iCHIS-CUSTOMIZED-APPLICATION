package org.dhis2.sensors.device_manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PairedDeviceRepository(
    private val storageManager: DeviceStorageManager,
) {
    private val _devices = MutableStateFlow(loadSortedDevices())
    val devices: StateFlow<List<SensorDevice>> = _devices.asStateFlow()

    fun refresh() {
        _devices.value = loadSortedDevices()
    }

    fun getPreferredDevice(deviceType: DeviceType): SensorDevice? {
        refresh()
        return _devices.value
            .filter { it.deviceType == deviceType && it.isPaired }
            .maxByOrNull { it.lastConnected }
    }

    fun findDevice(macAddress: String): SensorDevice? {
        refresh()
        return _devices.value.firstOrNull {
            it.macAddress.equals(macAddress.normalizedMacAddress(), ignoreCase = true)
        }
    }

    fun pairDevice(
        deviceName: String,
        macAddress: String,
        deviceType: DeviceType,
        lastConnected: Long = findDevice(macAddress)?.lastConnected ?: 0L,
    ) {
        val normalizedAddress = macAddress.normalizedMacAddress()
        val updatedDevices =
            loadSortedDevices()
                .filterNot { it.macAddress.equals(normalizedAddress, ignoreCase = true) }
                .plus(
                    SensorDevice(
                        deviceName = deviceName.ifBlank { deviceType.defaultDeviceName },
                        macAddress = normalizedAddress,
                        deviceType = deviceType,
                        lastConnected = lastConnected,
                        isPaired = true,
                    ),
                )
                .sortedWith(deviceComparator)

        persist(updatedDevices)
    }

    fun markDeviceConnected(
        deviceName: String,
        macAddress: String,
        deviceType: DeviceType,
    ) {
        pairDevice(
            deviceName = deviceName,
            macAddress = macAddress,
            deviceType = deviceType,
            lastConnected = System.currentTimeMillis(),
        )
    }

    fun removeDevice(macAddress: String) {
        val normalizedAddress = macAddress.normalizedMacAddress()
        persist(
            loadSortedDevices()
                .filterNot { it.macAddress.equals(normalizedAddress, ignoreCase = true) }
                .sortedWith(deviceComparator),
        )
    }

    private fun persist(devices: List<SensorDevice>) {
        storageManager.saveDevices(devices)
        _devices.value = devices
    }

    private fun loadSortedDevices(): List<SensorDevice> =
        storageManager.loadDevices().sortedWith(deviceComparator)

    private fun String.normalizedMacAddress(): String = trim().uppercase()

    private companion object {
        val deviceComparator =
            compareBy<SensorDevice>({ it.deviceType.ordinal })
                .thenByDescending { it.lastConnected }
                .thenBy { it.deviceName.lowercase() }
    }
}
