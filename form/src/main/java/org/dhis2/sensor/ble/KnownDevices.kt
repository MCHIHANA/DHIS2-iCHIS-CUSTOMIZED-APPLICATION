package org.dhis2.sensor.ble

/**
 * MAC addresses of the known medical sensors the app will auto-connect to.
 * The BLE scanner stops as soon as any of these is detected.
 */
object KnownDevices {
    /** FORA IR42 — Health Thermometer, Temperature Measurement (0x2A1C) */
    const val TEMP_SENSOR = "C0:26:DA:1B:06:A4"

    /** FORA O2 — Pulse Oximeter, custom Nordic service, SpO2 + Pulse Rate */
    const val SPO2_SENSOR = "C0:26:DA:17:D5:7D"

    /** Blood Pressure Monitor — Blood Pressure Measurement (0x2A35) */
    const val BP_SENSOR = "C0:26:DA:19:D4:FE"

    /** All known MACs as a set for fast lookup. */
    val ALL: Set<String> = setOf(TEMP_SENSOR, SPO2_SENSOR, BP_SENSOR)

    /** Returns the [SensorType] for a given MAC address. */
    fun typeFor(mac: String): SensorType = when (mac) {
        TEMP_SENSOR -> SensorType.TEMPERATURE
        SPO2_SENSOR -> SensorType.SPO2
        BP_SENSOR   -> SensorType.BLOOD_PRESSURE
        else        -> SensorType.UNKNOWN
    }
}
