package org.dhis2.usescases.vitaldashboard.model

/**
 * Vital Sign Types
 * 
 * Defines the types of vital signs monitored in the dashboard.
 * Each type includes:
 * - Display name
 * - Unit of measurement
 * - Normal range (for alert detection)
 * - Critical range (for critical alerts)
 * 
 * @author Shadreck Mkandawire
 */
enum class VitalSignType(
    val displayName: String,
    val unit: String,
    val normalRange: Pair<Float, Float>?,
    val criticalRange: Pair<Float, Float>?
) {
    BLOOD_PRESSURE(
        displayName = "Blood Pressure",
        unit = "mmHg",
        normalRange = 90f to 140f,  // Systolic
        criticalRange = 70f to 180f
    ),
    
    TEMPERATURE(
        displayName = "Temperature",
        unit = "°C",
        normalRange = 36.1f to 37.2f,
        criticalRange = 35.0f to 40.0f
    ),
    
    PULSE_RATE(
        displayName = "Pulse Rate",
        unit = "bpm",
        normalRange = 60f to 100f,
        criticalRange = 40f to 150f
    ),
    
    SPO2(
        displayName = "SpO₂",
        unit = "%",
        normalRange = 95f to 100f,
        criticalRange = 85f to 100f
    ),
    
    BLOOD_GLUCOSE(
        displayName = "Blood Glucose",
        unit = "mg/dL",
        normalRange = 70f to 140f,
        criticalRange = 40f to 300f
    ),
    
    RESPIRATORY_RATE(
        displayName = "Respiratory Rate",
        unit = "breaths/min",
        normalRange = 12f to 20f,
        criticalRange = 8f to 30f
    ),
    
    WEIGHT(
        displayName = "Weight",
        unit = "kg",
        normalRange = null,
        criticalRange = null
    ),
    
    HEIGHT(
        displayName = "Height",
        unit = "cm",
        normalRange = null,
        criticalRange = null
    );

    /**
     * Check if a value is within normal range
     */
    fun isNormal(value: Float): Boolean {
        return normalRange?.let { (min, max) ->
            value in min..max
        } ?: true
    }

    /**
     * Check if a value is critical
     */
    fun isCritical(value: Float): Boolean {
        return criticalRange?.let { (min, max) ->
            value < min || value > max
        } ?: false
    }

    /**
     * Get color for value status
     */
    fun getStatusColor(value: Float): VitalStatusColor {
        return when {
            isCritical(value) -> VitalStatusColor.CRITICAL
            !isNormal(value) -> VitalStatusColor.WARNING
            else -> VitalStatusColor.NORMAL
        }
    }
}

/**
 * Status colors for vital signs
 */
enum class VitalStatusColor {
    NORMAL,    // Green
    WARNING,   // Yellow/Orange
    CRITICAL   // Red
}
