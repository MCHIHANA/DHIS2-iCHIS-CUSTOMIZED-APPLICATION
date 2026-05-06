package org.dhis2.sensor.ble

/**
 * Identifies which type of sensor is being connected.
 * Used by [BleManager] and [FormViewModel] to route readings to the correct fields.
 */
enum class SensorType {
    TEMPERATURE,  // FORA IR42 — single value → temperature field
    SPO2,         // FORA O2   — two values  → SpO2 field + pulse rate field
    BLOOD_PRESSURE, // future  — two values  → systolic + diastolic fields
    UNKNOWN,
}
