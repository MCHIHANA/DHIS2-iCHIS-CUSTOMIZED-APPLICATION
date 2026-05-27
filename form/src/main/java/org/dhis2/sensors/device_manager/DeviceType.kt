package org.dhis2.sensors.device_manager

import org.dhis2.sensor.ble.SensorType

enum class DeviceType(
    val sectionTitle: String,
    val addActionLabel: String,
    val defaultDeviceName: String,
) {
    TEMPERATURE(
        sectionTitle = "Temperature Devices",
        addActionLabel = "Add New Thermometer",
        defaultDeviceName = "FORA Thermometer",
    ),
    BLOOD_PRESSURE(
        sectionTitle = "Blood Pressure Devices",
        addActionLabel = "Add New BP Device",
        defaultDeviceName = "FORA BP Monitor",
    ),
    SPO2(
        sectionTitle = "SpO2 Devices",
        addActionLabel = "Add New Oximeter",
        defaultDeviceName = "FORA Oximeter",
    ),
    GLUCOSE(
        sectionTitle = "Glucose Devices",
        addActionLabel = "Add New Glucose Device",
        defaultDeviceName = "Glucose Sensor",
    ),
    WIFI(
        sectionTitle = "WiFi Devices",
        addActionLabel = "Add New WiFi Device",
        defaultDeviceName = "WiFi Medical Device",
    ),
    USB(
        sectionTitle = "USB Devices",
        addActionLabel = "Add New USB Device",
        defaultDeviceName = "USB Medical Device",
    ),
}

fun DeviceType.toBleSensorType(): SensorType =
    when (this) {
        DeviceType.TEMPERATURE -> SensorType.TEMPERATURE
        DeviceType.BLOOD_PRESSURE -> SensorType.BLOOD_PRESSURE
        DeviceType.SPO2 -> SensorType.SPO2
        DeviceType.GLUCOSE -> SensorType.GLUCOSE
        DeviceType.WIFI, DeviceType.USB -> SensorType.UNKNOWN
    }

fun SensorType.toDeviceType(): DeviceType? =
    when (this) {
        SensorType.TEMPERATURE -> DeviceType.TEMPERATURE
        SensorType.BLOOD_PRESSURE -> DeviceType.BLOOD_PRESSURE
        SensorType.SPO2 -> DeviceType.SPO2
        SensorType.GLUCOSE -> DeviceType.GLUCOSE
        SensorType.UNKNOWN -> null
    }
