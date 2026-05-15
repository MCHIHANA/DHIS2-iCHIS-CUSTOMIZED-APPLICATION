package org.dhis2.sensors.core

import org.dhis2.sensors.devices.bloodpressure.BloodPressureSensorHandler
import org.dhis2.sensors.devices.glucose.GlucoseSensorHandler
import org.dhis2.sensors.devices.spo2.Spo2SensorHandler
import org.dhis2.sensors.devices.temperature.TemperatureSensorHandler

object SensorRegistry {
    private const val TEMP_SENSOR_MAC = "C0:26:DA:1B:06:A4"
    private const val SPO2_SENSOR_MAC = "C0:26:DA:17:D5:7D"
    private const val BP_SENSOR_MAC = "C0:26:DA:19:D4:FE"

    private val handlers: Map<SensorType, BaseSensorHandler> = mapOf(
        SensorType.TEMPERATURE to TemperatureSensorHandler(),
        SensorType.BLOOD_PRESSURE to BloodPressureSensorHandler(),
        SensorType.SPO2 to Spo2SensorHandler(),
        SensorType.GLUCOSE to GlucoseSensorHandler(),
    )

    private val sensorTypesByMac = mapOf(
        TEMP_SENSOR_MAC to SensorType.TEMPERATURE,
        SPO2_SENSOR_MAC to SensorType.SPO2,
        BP_SENSOR_MAC to SensorType.BLOOD_PRESSURE,
    )

    val knownDeviceAddresses: Set<String> = sensorTypesByMac.keys

    fun getHandler(sensorType: SensorType): BaseSensorHandler? = handlers[sensorType]

    fun getSensorType(deviceAddress: String): SensorType =
        sensorTypesByMac[deviceAddress.uppercase()] ?: SensorType.UNKNOWN
}
