package org.dhis2.sensor.ble

import org.dhis2.sensors.core.SensorRegistry

object KnownDevices {
    const val TEMP_SENSOR: String = SensorRegistry.TEMP_SENSOR_MAC
    const val SPO2_SENSOR: String = SensorRegistry.SPO2_SENSOR_MAC
    const val BP_SENSOR: String = SensorRegistry.BP_SENSOR_MAC

    val ALL: Set<String> = SensorRegistry.knownDeviceAddresses

    fun typeFor(mac: String): SensorType = SensorRegistry.getSensorType(mac)
}
