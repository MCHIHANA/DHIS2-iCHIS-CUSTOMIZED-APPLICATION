package org.dhis2.sensor.ble

/**
 * MAC addresses of the known medical sensors the app will auto-connect to.
 * The BLE scanner stops as soon as any of these is detected.
 */
object KnownDevices {
    /** Health Thermometer — Temperature Measurement (0x2A1C) */
    const val TEMP_SENSOR = "C0:26:DA:1B:06:A4"

    /** Pulse Oximeter — PLX Spot-check / Continuous (0x2A5E / 0x2A5F) */
    const val SPO2_SENSOR = "C0:26:DA:17:D5:7D"

    /** Blood Pressure Monitor — Blood Pressure Measurement (0x2A35) */
    const val BP_SENSOR = "C0:26:DA:19:D4:FE"

    /** All known MACs as a set for fast lookup. */
    val ALL: Set<String> = setOf(TEMP_SENSOR, SPO2_SENSOR, BP_SENSOR)
}
