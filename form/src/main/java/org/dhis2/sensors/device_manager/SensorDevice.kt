package org.dhis2.sensors.device_manager

data class SensorDevice(
    val deviceName: String,
    val macAddress: String,
    val deviceType: DeviceType,
    val lastConnected: Long,
    val isPaired: Boolean,
)
