package org.dhis2.sensors.device_manager

import org.dhis2.form.ui.sensor.SensorFieldResolver
import org.dhis2.sensor.config.SensorConfigRepository

class ReconnectManager(
    private val pairedDeviceRepository: PairedDeviceRepository,
) {
    fun resolvePreferredDevice(
        fieldUid: String,
        sensorConfigRepository: SensorConfigRepository,
    ): SensorDevice? =
        resolveDeviceType(fieldUid, sensorConfigRepository)?.let { deviceType ->
            pairedDeviceRepository.getPreferredDevice(deviceType)
        }

    fun resolvePreferredMacAddress(
        fieldUid: String,
        sensorConfigRepository: SensorConfigRepository,
        fallbackMacAddress: String? = null,
    ): String? =
        resolvePreferredDevice(fieldUid, sensorConfigRepository)?.macAddress ?: fallbackMacAddress

    fun resolveDeviceType(
        fieldUid: String,
        sensorConfigRepository: SensorConfigRepository,
    ): DeviceType? =
        SensorFieldResolver
            .resolveSensorType(fieldUid, sensorConfigRepository)
            .toDeviceType()
}
