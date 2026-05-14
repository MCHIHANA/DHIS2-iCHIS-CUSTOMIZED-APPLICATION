package org.dhis2.usescases.vitaldashboard.repository

import timber.log.Timber

data class VitalSignDefinition(
    val vitalSignType: org.dhis2.usescases.vitaldashboard.model.VitalSignType,
    val measurementKind: VitalMeasurementKind,
    val unit: String,
)

/**
 * Maps DHIS2 data element UIDs to vital sign definitions.
 *
 * This class is intentionally offline-first and server-independent.
 * All UIDs are hardcoded to match the project's DHIS2 metadata — no DataStore
 * lookup, no network call, no SharedPreferences cache needed.
 *
 * UIDs come from CORRECTED_DATASTORE_CONFIG.json and FormViewModel.bpFieldMap:
 *   Temperature      → KXNH45ts16S
 *   SpO2             → VqwQWWDmYLn
 *   Pulse (oximeter) → tZbUrUbhUNy
 *   Pulse (BP)       → S7OjKl85YSh
 *   BP Systolic      → HkfzcXMdLLF
 *   BP Diastolic     → BaGxiB8AsNI  (also skBarAsIYIL as legacy alias)
 */
class VitalSignConfig {

    /**
     * No-op — kept for API compatibility with [VitalDashboardRepository].
     * All mappings are hardcoded; no refresh from server is required.
     */
    suspend fun refresh() {
        Timber.d("VitalSignConfig: using hardcoded offline mappings — no server call needed.")
    }

    fun isVitalSignDataElement(dataElementUid: String?): Boolean =
        dataElementUid != null && HARDCODED_MAPPINGS.containsKey(dataElementUid)

    fun getVitalSignDefinition(dataElementUid: String?): VitalSignDefinition? =
        dataElementUid?.let { HARDCODED_MAPPINGS[it] }

    fun getAllDataElementUids(): Set<String> = HARDCODED_MAPPINGS.keys

    companion object {
        /**
         * Single source of truth for all sensor data element UIDs.
         * Update this map if new sensors or data elements are added to the project.
         */
        val HARDCODED_MAPPINGS: Map<String, VitalSignDefinition> = mapOf(

            // ── Temperature sensor (MAC: C0:26:DA:1B:06:A4) ──────────────────
            "KXNH45ts16S" to VitalSignDefinition(
                vitalSignType = VitalMeasurementKind.TEMPERATURE.vitalSignType,
                measurementKind = VitalMeasurementKind.TEMPERATURE,
                unit = "°C",
            ),

            // ── Pulse Oximeter (MAC: C0:26:DA:17:D5:7D) ──────────────────────
            "VqwQWWDmYLn" to VitalSignDefinition(
                vitalSignType = VitalMeasurementKind.SPO2.vitalSignType,
                measurementKind = VitalMeasurementKind.SPO2,
                unit = "%",
            ),
            "tZbUrUbhUNy" to VitalSignDefinition(
                vitalSignType = VitalMeasurementKind.PULSE_RATE.vitalSignType,
                measurementKind = VitalMeasurementKind.PULSE_RATE,
                unit = "bpm",
            ),

            // ── Blood Pressure sensor (MAC: C0:26:DA:19:D4:FE) ───────────────
            "HkfzcXMdLLF" to VitalSignDefinition(
                vitalSignType = VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC.vitalSignType,
                measurementKind = VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC,
                unit = "mmHg",
            ),
            "BaGxiB8AsNI" to VitalSignDefinition(
                vitalSignType = VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC.vitalSignType,
                measurementKind = VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC,
                unit = "mmHg",
            ),
            "S7OjKl85YSh" to VitalSignDefinition(
                vitalSignType = VitalMeasurementKind.PULSE_RATE.vitalSignType,
                measurementKind = VitalMeasurementKind.PULSE_RATE,
                unit = "bpm",
            ),

            // ── Legacy / alternate UIDs ───────────────────────────────────────
            "skBarAsIYIL" to VitalSignDefinition(
                vitalSignType = VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC.vitalSignType,
                measurementKind = VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC,
                unit = "mmHg",
            ),
        )
    }
}
