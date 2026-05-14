package org.dhis2.usescases.vitaldashboard.model

enum class VitalSignType(
    val displayName: String,
    val unit: String,
    val normalRange: Pair<Float, Float>?,
    val criticalRange: Pair<Float, Float>?,
) {
    BLOOD_PRESSURE(
        displayName = "Blood Pressure",
        unit = "mmHg",
        normalRange = 90f to 140f,
        criticalRange = 70f to 180f,
    ),
    TEMPERATURE(
        displayName = "Temperature",
        unit = "C",
        normalRange = 36.1f to 37.2f,
        criticalRange = 35f to 40f,
    ),
    PULSE_RATE(
        displayName = "Pulse Rate",
        unit = "bpm",
        normalRange = 60f to 100f,
        criticalRange = 40f to 150f,
    ),
    SPO2(
        displayName = "SpO2",
        unit = "%",
        normalRange = 95f to 100f,
        criticalRange = 85f to 100f,
    ),
    BLOOD_GLUCOSE(
        displayName = "Blood Glucose",
        unit = "mg/dL",
        normalRange = 70f to 140f,
        criticalRange = 40f to 300f,
    ),
    RESPIRATORY_RATE(
        displayName = "Respiratory Rate",
        unit = "breaths/min",
        normalRange = 12f to 20f,
        criticalRange = 8f to 30f,
    ),
    WEIGHT(
        displayName = "Weight",
        unit = "kg",
        normalRange = null,
        criticalRange = null,
    ),
    HEIGHT(
        displayName = "Height",
        unit = "cm",
        normalRange = null,
        criticalRange = null,
    ),
}

enum class VitalStatusColor {
    NORMAL,
    WARNING,
    CRITICAL,
}
