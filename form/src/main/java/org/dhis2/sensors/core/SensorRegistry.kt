package org.dhis2.sensors.core

import org.dhis2.sensors.devices.bloodpressure.BloodPressureSensorHandler
import org.dhis2.sensors.devices.glucose.GlucoseSensorHandler
import org.dhis2.sensors.devices.spo2.Spo2SensorHandler
import org.dhis2.sensors.devices.temperature.TemperatureSensorHandler

object SensorRegistry {
    const val TEMP_SENSOR_MAC: String = "C0:26:DA:1B:06:A4"
    const val SPO2_SENSOR_MAC: String = "C0:26:DA:17:D5:7D"
    const val BP_SENSOR_MAC: String = "C0:26:DA:19:D4:FE"

    private val sensorTypesByMac = mapOf(
        TEMP_SENSOR_MAC to SensorType.TEMPERATURE,
        SPO2_SENSOR_MAC to SensorType.SPO2,
        BP_SENSOR_MAC to SensorType.BLOOD_PRESSURE,
    )

    val knownDeviceAddresses: Set<String> = sensorTypesByMac.keys

    fun getHandler(sensorType: SensorType): BaseSensorHandler? = when (sensorType) {
        SensorType.TEMPERATURE -> TemperatureSensorHandler()
        SensorType.BLOOD_PRESSURE -> BloodPressureSensorHandler()
        SensorType.SPO2 -> Spo2SensorHandler()
        SensorType.GLUCOSE -> GlucoseSensorHandler()
        SensorType.UNKNOWN -> null
    }

    fun getSensorType(deviceAddress: String): SensorType =
        sensorTypesByMac[deviceAddress.uppercase()] ?: SensorType.UNKNOWN
}
