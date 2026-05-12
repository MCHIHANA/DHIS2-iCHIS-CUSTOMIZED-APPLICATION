package org.dhis2.usescases.vitaldashboard.repository

import org.dhis2.usescases.vitaldashboard.model.VitalSignType

/**
 * Vital Sign Configuration
 * 
 * Maps DHIS2 data element UIDs to vital sign types.
 * This configuration should be customized based on your DHIS2 instance.
 * 
 * Configuration can be loaded from:
 * - DHIS2 datastore
 * - Local configuration file
 * - Hardcoded mappings (as shown below)
 * 
 * @author Shadreck Mkandawire
 */
class VitalSignConfig {

    /**
     * Map of data element UID to vital sign type
     * 
     * TODO: Replace these UIDs with actual data element UIDs from your DHIS2 instance
     * These can be configured in DHIS2 datastore or loaded from a configuration file
     */
    private val dataElementMapping = mapOf(
        // Blood Pressure
        "HkfzcXMdLLF" to VitalSignType.BLOOD_PRESSURE,  // Systolic
        "skBarAsIYIL" to VitalSignType.BLOOD_PRESSURE,  // Diastolic (from BP_SENSOR_IMPLEMENTATION.md)
        
        // Temperature
        "TEMP_DATA_ELEMENT_UID" to VitalSignType.TEMPERATURE,
        
        // Pulse Rate
        "tZbUrUbhUNy" to VitalSignType.PULSE_RATE,  // From BP_SENSOR_IMPLEMENTATION.md
        
        // SpO2
        "VqwQWWDmYLn" to VitalSignType.SPO2,  // From TROUBLESHOOTING_OXIMETER.md
        
        // Blood Glucose
        "GLUCOSE_DATA_ELEMENT_UID" to VitalSignType.BLOOD_GLUCOSE,
        
        // Respiratory Rate
        "RESPIRATORY_RATE_UID" to VitalSignType.RESPIRATORY_RATE,
        
        // Weight
        "WEIGHT_DATA_ELEMENT_UID" to VitalSignType.WEIGHT,
        
        // Height
        "HEIGHT_DATA_ELEMENT_UID" to VitalSignType.HEIGHT
    )

    /**
     * Check if a data element UID is a vital sign
     */
    fun isVitalSignDataElement(dataElementUid: String?): Boolean {
        return dataElementUid != null && dataElementMapping.containsKey(dataElementUid)
    }

    /**
     * Get vital sign type for a data element UID
     */
    fun getVitalSignType(dataElementUid: String?): VitalSignType? {
        return dataElementUid?.let { dataElementMapping[it] }
    }

    /**
     * Get all configured data element UIDs
     */
    fun getAllDataElementUids(): Set<String> {
        return dataElementMapping.keys
    }

    /**
     * Get data element UIDs for a specific vital sign type
     */
    fun getDataElementUids(vitalSignType: VitalSignType): List<String> {
        return dataElementMapping
            .filter { it.value == vitalSignType }
            .keys
            .toList()
    }

    companion object {
        /**
         * Create configuration from DHIS2 datastore
         * 
         * This method can be used to load configuration dynamically from DHIS2 datastore
         * instead of using hardcoded mappings.
         * 
         * Example datastore structure:
         * {
         *   "vitalSignMappings": {
         *     "bloodPressureSystolic": "HkfzcXMdLLF",
         *     "bloodPressureDiastolic": "skBarAsIYIL",
         *     "temperature": "TEMP_UID",
         *     "pulseRate": "tZbUrUbhUNy",
         *     "spo2": "VqwQWWDmYLn",
         *     "glucose": "GLUCOSE_UID"
         *   }
         * }
         */
        fun fromDataStore(datastoreJson: String): VitalSignConfig {
            // TODO: Implement datastore parsing
            return VitalSignConfig()
        }
    }
}
