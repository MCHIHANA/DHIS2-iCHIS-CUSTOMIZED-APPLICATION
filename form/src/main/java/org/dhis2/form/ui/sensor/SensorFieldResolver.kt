package org.dhis2.form.ui.sensor

import org.dhis2.form.model.FieldUiModel
import org.dhis2.sensor.ble.KnownDevices
import org.dhis2.sensor.ble.SensorType
import org.dhis2.sensor.config.SensorConfig
import org.dhis2.sensor.config.SensorConfigRepository

object SensorFieldResolver {
    const val TEMPERATURE_UID = "KXNH45ts16S"
    const val SPO2_UID = "VqwQWWDmYLn"
    const val SPO2_PULSE_UID = "tZbUrUbhUNy"
    const val BLOOD_PRESSURE_PULSE_UID = "S7OjKl85YSh"
    const val SYSTOLIC_UID = "HkfzcXMdLLF"
    const val DIASTOLIC_UID = "BaGxiB8AsNI"

    val knownSensorFieldUids: Set<String> =
        setOf(
            TEMPERATURE_UID,
            SPO2_UID,
            SPO2_PULSE_UID,
            BLOOD_PRESSURE_PULSE_UID,
            SYSTOLIC_UID,
            DIASTOLIC_UID,
        )

    fun resolveSecondaryFieldUid(
        uid: String,
        currentItems: List<FieldUiModel>,
    ): String? =
        when (uid) {
            SPO2_UID -> SPO2_PULSE_UID
            SPO2_PULSE_UID -> SPO2_UID
            else -> resolveSecondaryFromLabels(uid, currentItems)
        }

    fun resolvePreferredMacAddress(
        uid: String,
        sensorConfigRepository: SensorConfigRepository,
    ): String? {
        val sensorConfig = sensorConfigRepository.getConfigByDataElement(uid)
        return resolvePreferredMacAddress(uid, sensorConfig)
    }

    fun resolvePreferredMacAddress(
        uid: String,
        sensorConfig: SensorConfig?,
    ): String? =
        sensorConfig?.macAddress?.takeIf { it.isNotBlank() } ?: when (resolveSensorType(uid, sensorConfig)) {
            SensorType.TEMPERATURE -> KnownDevices.TEMP_SENSOR
            SensorType.SPO2 -> KnownDevices.SPO2_SENSOR
            SensorType.BLOOD_PRESSURE -> KnownDevices.BP_SENSOR
            else -> null
        }

    fun resolveSensorType(
        uid: String,
        sensorConfigRepository: SensorConfigRepository,
    ): SensorType {
        val sensorConfig = sensorConfigRepository.getConfigByDataElement(uid)
        return resolveSensorType(uid, sensorConfig)
    }

    fun resolveSensorType(
        uid: String,
        sensorConfig: SensorConfig?,
    ): SensorType {
        val normalizedName = sensorConfig?.name.orEmpty().lowercase()
        if ("blood pressure" in normalizedName) {
            return SensorType.BLOOD_PRESSURE
        }
        if ("temperature" in normalizedName || "thermometer" in normalizedName) {
            return SensorType.TEMPERATURE
        }
        if ("spo2" in normalizedName || "oxygen" in normalizedName || "oximeter" in normalizedName) {
            return SensorType.SPO2
        }

        return when (uid) {
            TEMPERATURE_UID -> SensorType.TEMPERATURE
            SPO2_UID, SPO2_PULSE_UID -> SensorType.SPO2
            BLOOD_PRESSURE_PULSE_UID, SYSTOLIC_UID, DIASTOLIC_UID -> SensorType.BLOOD_PRESSURE
            else -> SensorType.UNKNOWN
        }
    }

    fun hasCompletedReading(status: String?): Boolean =
        SensorStatusText.isCompleted(status)

    private fun resolveSecondaryFromLabels(
        uid: String,
        currentItems: List<FieldUiModel>,
    ): String? {
        val tappedField = currentItems.firstOrNull { it.uid == uid } ?: return null
        return when {
            tappedField.label.contains("spo2", ignoreCase = true) ||
                tappedField.label.contains("oxygen", ignoreCase = true) ->
                currentItems.firstOrNull { item ->
                    item.uid != uid && (
                        item.label.contains("pulse", ignoreCase = true) ||
                            item.label.contains("bpm", ignoreCase = true)
                    )
                }?.uid

            tappedField.label.contains("pulse", ignoreCase = true) ||
                tappedField.label.contains("bpm", ignoreCase = true) ->
                currentItems.firstOrNull { item ->
                    item.uid != uid && (
                        item.label.contains("spo2", ignoreCase = true) ||
                            item.label.contains("oxygen", ignoreCase = true)
                    )
                }?.uid

            else -> null
        }
    }
}
