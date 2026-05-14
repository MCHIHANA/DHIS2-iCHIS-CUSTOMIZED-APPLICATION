package org.dhis2.usescases.vitaldashboard.repository

import org.dhis2.usescases.vitaldashboard.model.VitalSignType

enum class VitalMeasurementKind(
    val vitalSignType: VitalSignType,
    val label: String,
) {
    BLOOD_PRESSURE_SYSTOLIC(
        vitalSignType = VitalSignType.BLOOD_PRESSURE,
        label = "Systolic",
    ),
    BLOOD_PRESSURE_DIASTOLIC(
        vitalSignType = VitalSignType.BLOOD_PRESSURE,
        label = "Diastolic",
    ),
    TEMPERATURE(
        vitalSignType = VitalSignType.TEMPERATURE,
        label = "Temperature",
    ),
    PULSE_RATE(
        vitalSignType = VitalSignType.PULSE_RATE,
        label = "Pulse Rate",
    ),
    SPO2(
        vitalSignType = VitalSignType.SPO2,
        label = "SpO2",
    ),
    BLOOD_GLUCOSE(
        vitalSignType = VitalSignType.BLOOD_GLUCOSE,
        label = "Blood Glucose",
    ),
    RESPIRATORY_RATE(
        vitalSignType = VitalSignType.RESPIRATORY_RATE,
        label = "Respiratory Rate",
    ),
    WEIGHT(
        vitalSignType = VitalSignType.WEIGHT,
        label = "Weight",
    ),
    HEIGHT(
        vitalSignType = VitalSignType.HEIGHT,
        label = "Height",
    ),
}
