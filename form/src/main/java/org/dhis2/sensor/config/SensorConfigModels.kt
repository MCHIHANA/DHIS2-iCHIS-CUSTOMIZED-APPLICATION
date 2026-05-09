package org.dhis2.sensor.config

import com.google.gson.annotations.SerializedName

data class SensorConfigResponse(
    @SerializedName("sensors")
    val sensors: List<SensorConfig>
)

/**
 * Sensor configuration model supporting both single-value and multi-value sensors.
 *
 * **New Architecture (multi-measurement):**
 * ```json
 * {
 *   "name": "Blood Pressure",
 *   "type": "multi",
 *   "serviceUUID": "00001810-0000-1000-8000-00805f9b34fb",
 *   "characteristicUUID": "00002A35-0000-1000-8000-00805f9b34fb",
 *   "macAddress": "C0:26:DA:19:D4:FE",
 *   "measurements": {
 *     "systolic": {"dataElement": "HkfzcXMdLLF", "unit": "mmHg"},
 *     "diastolic": {"dataElement": "skBarAsIYIL", "unit": "mmHg"},
 *     "pulse": {"dataElement": "tZbUrUbhUNy", "unit": "bpm"}
 *   }
 * }
 * ```
 *
 * **Legacy Support (deprecated):**
 * - Single-value: `dataElement` + `unit`
 * - Dual-value: `dataElements` (systolic/diastolic only)
 */
data class SensorConfig(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("type")
    val type: String? = null, // "single" or "multi"
    
    @SerializedName("macAddress")
    val macAddress: String? = null,
    
    // ── New multi-measurement architecture ──
    @SerializedName("measurements")
    val measurements: Map<String, MeasurementConfig>? = null,
    
    // ── Legacy fields (deprecated but supported for backward compatibility) ──
    @SerializedName("dataElement")
    val dataElement: String? = null,
    
    @SerializedName("dataElements")
    val dataElements: BloodPressureElements? = null,
    
    @SerializedName("unit")
    val unit: String? = null,
    
    // ── BLE configuration ──
    @SerializedName("serviceUUID")
    val serviceUUID: String? = null,
    
    @SerializedName("characteristicUUID")
    val characteristicUUID: String? = null,
    
    // ── UI behavior ──
    @SerializedName("sensorRequired")
    val sensorRequired: Boolean = false,
    
    @SerializedName("manualAllowed")
    val manualAllowed: Boolean = true
) {
    /**
     * Returns true if this sensor uses the new multi-measurement architecture.
     */
    fun isMultiMeasurement(): Boolean = !measurements.isNullOrEmpty()
    
    /**
     * Returns true if this is a legacy dual-value sensor (systolic/diastolic only).
     */
    fun isLegacyDualValue(): Boolean = dataElements != null
    
    /**
     * Returns true if this is a legacy single-value sensor.
     */
    fun isLegacySingleValue(): Boolean = dataElement != null && dataElements == null
}

/**
 * Configuration for a single measurement within a multi-measurement sensor.
 */
data class MeasurementConfig(
    @SerializedName("dataElement")
    val dataElement: String,
    
    @SerializedName("unit")
    val unit: String
)

/**
 * Legacy dual-value structure (deprecated).
 * Kept for backward compatibility with existing configurations.
 */
data class BloodPressureElements(
    @SerializedName("systolic")
    val systolic: String,
    
    @SerializedName("diastolic")
    val diastolic: String
)
