package org.dhis2.usescases.vitaldashboard.repository

import kotlinx.coroutines.withContext
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.usescases.vitaldashboard.AlertType
import org.dhis2.usescases.vitaldashboard.PatientVitalSummary
import org.dhis2.usescases.vitaldashboard.RecentVitalReading
import org.dhis2.usescases.vitaldashboard.TrendDataPoint
import org.dhis2.usescases.vitaldashboard.TrendSeries
import org.dhis2.usescases.vitaldashboard.VitalAlert
import org.dhis2.usescases.vitaldashboard.VitalDashboardData
import org.dhis2.usescases.vitaldashboard.VitalDashboardFilter
import org.dhis2.usescases.vitaldashboard.VitalMeasurement
import org.dhis2.usescases.vitaldashboard.VitalStatistics
import org.dhis2.usescases.vitaldashboard.VitalValue
import org.dhis2.usescases.vitaldashboard.model.VitalSignType
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import timber.log.Timber
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import java.util.Date

class VitalDashboardRepository(
    private val d2: D2,
    private val dispatchers: Dispatcher,
    private val vitalSignConfig: VitalSignConfig,
) {

    suspend fun isUserAuthorized(): Boolean = true

    suspend fun getDashboardData(filter: VitalDashboardFilter): VitalDashboardData =
        withContext(dispatchers.io) {
            vitalSignConfig.refresh()

            val events = fetchVitalSignEvents(filter)
            val dataElementsByUid = fetchDataElementsMetadata(events)
            val patients = fetchPatients(events)
            val attributesByUid = fetchAttributesMetadata(patients)
            val measurements =
                applyFilter(
                    measurements = processEventsToMeasurements(events, patients, attributesByUid, dataElementsByUid),
                    filter = filter,
                )

            VitalDashboardData(
                patientSummaries = generatePatientSummaries(measurements, patients, attributesByUid),
                recentMeasurements = buildRecentReadings(measurements),
                alerts = detectAlerts(measurements),
                statistics = calculateStatistics(measurements),
                trends = generateTrends(measurements),
            )
        }

    private fun fetchVitalSignEvents(filter: VitalDashboardFilter): List<Event> {
        // Start with all non-deleted events. We intentionally do NOT filter by sync
        // state so that locally-saved (TO_POST / TO_UPDATE) events are included —
        // the dashboard must work offline with unsynced readings.
        //
        // IMPORTANT: .withTrackedEntityDataValues() must be called so the SDK
        // eagerly loads data values from their separate DB table. Without it,
        // event.trackedEntityDataValues() returns null for every event.
        var eventQuery = d2.eventModule().events()
            .byDeleted().isFalse
            .withTrackedEntityDataValues()

        // Date filters must be applied to the same query chain (each call returns
        // a new repository object; the result must be reassigned).
        filter.startDate?.let { startDate ->
            eventQuery = eventQuery.byEventDate().afterOrEqual(Date(startDate))
        }
        filter.endDate?.let { endDate ->
            eventQuery = eventQuery.byEventDate().beforeOrEqual(Date(endDate))
        }

        val allEvents = eventQuery.blockingGet()

        Timber.d("DASHBOARD_DEBUG: Total non-deleted events in local DB: %s", allEvents.size)
        allEvents.forEach { event ->
            Timber.d(
                "DASHBOARD_DEBUG: event=%s program=%s state=%s dataValues=%s",
                event.uid(),
                event.program(),
                event.state(),
                event.trackedEntityDataValues()?.size ?: 0,
            )
            event.trackedEntityDataValues()?.forEach { dv ->
                Timber.d(
                    "DASHBOARD_DEBUG:   DE=%s VALUE=%s",
                    dv.dataElement(),
                    dv.value(),
                )
            }
        }

        // Keep only events that actually carry data values.
        return allEvents.filter { event ->
            event.trackedEntityDataValues().orEmpty().isNotEmpty()
        }.also { filtered ->
            Timber.d("DASHBOARD_DEBUG: Events with data values: %s", filtered.size)
        }
    }

    private fun fetchDataElementsMetadata(events: List<Event>): Map<String, DataElement> {
        val dataElementUids =
            events.flatMap { event ->
                event.trackedEntityDataValues().orEmpty().mapNotNull { dataValue -> dataValue.dataElement() }
            }.distinct()

        if (dataElementUids.isEmpty()) {
            return emptyMap()
        }

        return d2.dataElementModule().dataElements()
            .byUid().`in`(dataElementUids)
            .blockingGet()
            .associateBy { it.uid() }
    }

    private fun fetchPatients(events: List<Event>): Map<String, TrackedEntityInstance> {
        val teiUids = events.mapNotNull { resolveTrackedEntityInstance(it) }.distinct()
        if (teiUids.isEmpty()) {
            return emptyMap()
        }

        return d2.trackedEntityModule().trackedEntityInstances()
            .byUid().`in`(teiUids)
            .withTrackedEntityAttributeValues()
            .blockingGet()
            .associateBy { it.uid() }
    }

    private fun fetchAttributesMetadata(
        patients: Map<String, TrackedEntityInstance>,
    ): Map<String, TrackedEntityAttribute> {
        val attributeUids =
            patients.values
                .flatMap { tei -> tei.trackedEntityAttributeValues().orEmpty() }
                .mapNotNull { attributeValue -> attributeValue.trackedEntityAttribute() }
                .distinct()

        if (attributeUids.isEmpty()) {
            return emptyMap()
        }

        return d2.trackedEntityModule().trackedEntityAttributes()
            .byUid().`in`(attributeUids)
            .blockingGet()
            .associateBy { it.uid() }
    }

    private fun processEventsToMeasurements(
        events: List<Event>,
        patients: Map<String, TrackedEntityInstance>,
        attributesByUid: Map<String, TrackedEntityAttribute>,
        dataElementsByUid: Map<String, DataElement>,
    ): List<VitalMeasurement> {
        val measurements = mutableListOf<VitalMeasurement>()

        // Debug: log what attributes are available for each patient
        patients.forEach { (uid, tei) ->
            val attrs = tei.trackedEntityAttributeValues().orEmpty()
            Timber.d("PATIENT_DEBUG: tei=%s attributeCount=%s", uid.take(8), attrs.size)
            attrs.forEach { av ->
                val attrName = attributesByUid[av.trackedEntityAttribute()]?.displayName() ?: av.trackedEntityAttribute()
                Timber.d("PATIENT_DEBUG:   attr='%s' value='%s'", attrName, av.value())
            }
        }

        events.forEach { event ->
            val trackedEntityUid = resolveTrackedEntityInstance(event)
            val patientUid = trackedEntityUid ?: syntheticPatientUid(event)
            val patient = trackedEntityUid?.let { patients[it] }
            val patientName =
                patient?.let { getPatientName(it, attributesByUid) }
                    ?: buildFallbackPatientName(event)

            event.trackedEntityDataValues()?.forEach { dataValue ->
                val definition =
                    resolveVitalSignDefinition(
                        dataElementUid = dataValue.dataElement(),
                        dataElementsByUid = dataElementsByUid,
                    ) ?: return@forEach
                val value = parseVitalValue(dataValue.value(), definition.unit)
                measurements.add(
                    VitalMeasurement(
                        uid = "${event.uid()}_${dataValue.dataElement()}",
                        eventUid = event.uid(),
                        patientUid = patientUid,
                        patientName = patientName,
                        vitalSignType = definition.vitalSignType,
                        measurementKind = definition.measurementKind,
                        value = value,
                        timestamp = event.eventDate()?.time ?: event.created()?.time ?: System.currentTimeMillis(),
                        lastUpdatedTimestamp = event.lastUpdated()?.time ?: event.created()?.time ?: event.eventDate()?.time ?: System.currentTimeMillis(),
                        isAbnormal = isValueAbnormal(value, definition.measurementKind),
                        notes = null,
                    ),
                )
            }
        }

        Timber.d("Vital dashboard processed %s measurements from %s events", measurements.size, events.size)
        return measurements
    }

    private fun applyFilter(
        measurements: List<VitalMeasurement>,
        filter: VitalDashboardFilter,
    ): List<VitalMeasurement> =
        measurements.filter { measurement ->
            val matchesPatient = filter.patientUid == null || measurement.patientUid == filter.patientUid
            val matchesVitalType = filter.vitalSignType == null || measurement.vitalSignType == filter.vitalSignType
            val matchesAlert = !filter.showAlertsOnly || measurement.isAbnormal
            matchesPatient && matchesVitalType && matchesAlert
        }

    private fun generatePatientSummaries(
        measurements: List<VitalMeasurement>,
        patients: Map<String, TrackedEntityInstance>,
        attributesByUid: Map<String, TrackedEntityAttribute>,
    ): List<PatientVitalSummary> =
        measurements.groupBy { it.patientUid }
            .map { (patientUid, patientMeasurements) ->
                val patient = patients[patientUid]
                PatientVitalSummary(
                    patientUid = patientUid,
                    patientName = patient?.let { getPatientName(it, attributesByUid) } ?: "Unknown Patient",
                    age = patient?.let { getPatientAge(it, attributesByUid) },
                    gender = patient?.let { getPatientGender(it, attributesByUid) },
                    latestVitals = buildVitalValueMap(patientMeasurements),
                    hasAlerts = patientMeasurements.any { it.isAbnormal },
                    lastMeasurementTime = patientMeasurements.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis(),
                    lastUpdatedTime = patientMeasurements.maxOfOrNull { it.lastUpdatedTimestamp } ?: System.currentTimeMillis(),
                )
            }.sortedByDescending { it.lastMeasurementTime }

    private fun buildRecentReadings(measurements: List<VitalMeasurement>): List<RecentVitalReading> =
        measurements.groupBy { it.eventUid }
            .values
            .map { eventMeasurements ->
                val latestMeasurement = eventMeasurements.maxByOrNull { it.timestamp }!!
                RecentVitalReading(
                    eventUid = latestMeasurement.eventUid,
                    patientUid = latestMeasurement.patientUid,
                    patientName = latestMeasurement.patientName,
                    timestamp = latestMeasurement.timestamp,
                    lastUpdatedTimestamp = eventMeasurements.maxOfOrNull { it.lastUpdatedTimestamp } ?: latestMeasurement.timestamp,
                    values = buildVitalValueMap(eventMeasurements),
                    hasAlerts = eventMeasurements.any { it.isAbnormal },
                )
            }.sortedByDescending { it.timestamp }
            .take(50)

    private fun buildVitalValueMap(measurements: List<VitalMeasurement>): Map<VitalSignType, VitalValue> {
        val latestByKind =
            measurements
                .groupBy { it.measurementKind }
                .mapValues { (_, kindMeasurements) ->
                    kindMeasurements.maxByOrNull { it.timestamp }
                }

        val latestVitals = linkedMapOf<VitalSignType, VitalValue>()

        val systolic = latestByKind[VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC]?.value
        val diastolic = latestByKind[VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC]?.value
        buildBloodPressureValue(systolic, diastolic)?.let { bloodPressureValue ->
            latestVitals[VitalSignType.BLOOD_PRESSURE] = bloodPressureValue
        }

        latestByKind.values
            .filterNotNull()
            .filter { it.measurementKind !in bloodPressureKinds }
            .sortedBy { it.vitalSignType.ordinal }
            .forEach { measurement ->
                latestVitals[measurement.vitalSignType] = measurement.value
            }

        return latestVitals
    }

    private fun buildBloodPressureValue(
        systolic: VitalValue?,
        diastolic: VitalValue?,
    ): VitalValue? {
        if (systolic == null && diastolic == null) {
            return null
        }

        val systolicValue = systolic?.value ?: "--"
        val diastolicValue = diastolic?.value ?: "--"
        return VitalValue(
            value = "$systolicValue/$diastolicValue",
            unit = "mmHg",
            numericValue = systolic?.numericValue,
        )
    }

    private fun detectAlerts(measurements: List<VitalMeasurement>): List<VitalAlert> =
        measurements.filter { it.isAbnormal }
            .map { measurement ->
                val alertType = determineAlertType(measurement.value, measurement.measurementKind)
                VitalAlert(
                    uid = measurement.uid,
                    patientUid = measurement.patientUid,
                    patientName = measurement.patientName,
                    vitalSignType = measurement.vitalSignType,
                    alertType = alertType,
                    value = measurement.value,
                    timestamp = measurement.timestamp,
                    message = generateAlertMessage(measurement, alertType),
                )
            }.sortedByDescending { it.timestamp }

    private fun calculateStatistics(
        measurements: List<VitalMeasurement>,
    ): VitalStatistics {
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        val averagesByType =
            measurements
                .groupBy { it.vitalSignType }
                .mapValues { (_, typeMeasurements) ->
                    typeMeasurements.mapNotNull { it.value.numericValue?.toDouble() }.average()
                }.filterValues { !it.isNaN() }

        return VitalStatistics(
            totalPatients = measurements.map { it.patientUid }.distinct().size,
            totalMeasurements = measurements.size,
            activeAlerts = measurements.count { it.isAbnormal && it.timestamp >= oneDayAgo },
            measurementsToday = measurements.count { it.timestamp >= oneDayAgo },
            averagesByType = averagesByType,
        )
    }

    private fun generateTrends(measurements: List<VitalMeasurement>): Map<VitalSignType, List<TrendSeries>> =
        measurements.groupBy { it.vitalSignType }
            .mapValues { (_, vitalMeasurements) ->
                vitalMeasurements
                    .groupBy { it.measurementKind }
                    .map { (kind, kindMeasurements) ->
                        TrendSeries(
                            label = kind.label,
                            points =
                                kindMeasurements
                                    .sortedBy { it.timestamp }
                                    .mapNotNull { measurement ->
                                        measurement.value.numericValue?.let { numericValue ->
                                            TrendDataPoint(
                                                timestamp = measurement.timestamp,
                                                value = numericValue,
                                                isAbnormal = measurement.isAbnormal,
                                            )
                                        }
                                    },
                        )
                    }.filter { it.points.isNotEmpty() }
            }

    private fun parseVitalValue(
        valueString: String?,
        unit: String,
    ): VitalValue =
        if (valueString.isNullOrBlank()) {
            VitalValue("--", unit, null)
        } else {
            VitalValue(
                value = valueString,
                unit = unit,
                numericValue = valueString.toFloatOrNull(),
            )
        }

    private fun isValueAbnormal(
        value: VitalValue,
        measurementKind: VitalMeasurementKind,
    ): Boolean {
        val numericValue = value.numericValue ?: return false
        val range = normalRangeFor(measurementKind) ?: return false
        return numericValue < range.first || numericValue > range.second
    }

    private fun determineAlertType(
        value: VitalValue,
        measurementKind: VitalMeasurementKind,
    ): AlertType {
        val numericValue = value.numericValue ?: return AlertType.ABNORMAL
        val normalRange = normalRangeFor(measurementKind) ?: return AlertType.ABNORMAL
        val criticalRange = criticalRangeFor(measurementKind)

        return when {
            criticalRange != null && numericValue < criticalRange.first -> AlertType.CRITICAL_LOW
            criticalRange != null && numericValue > criticalRange.second -> AlertType.CRITICAL_HIGH
            numericValue < normalRange.first -> AlertType.LOW
            numericValue > normalRange.second -> AlertType.HIGH
            else -> AlertType.ABNORMAL
        }
    }

    private fun generateAlertMessage(
        measurement: VitalMeasurement,
        alertType: AlertType,
    ): String {
        val severity =
            when (alertType) {
                AlertType.CRITICAL_HIGH, AlertType.CRITICAL_LOW -> "Critical"
                AlertType.HIGH, AlertType.LOW -> "Warning"
                AlertType.ABNORMAL -> "Alert"
            }
        val direction =
            when (alertType) {
                AlertType.HIGH, AlertType.CRITICAL_HIGH -> "High"
                AlertType.LOW, AlertType.CRITICAL_LOW -> "Low"
                AlertType.ABNORMAL -> "Abnormal"
            }

        val label =
            if (measurement.measurementKind in bloodPressureKinds) {
                "${measurement.vitalSignType.displayName} (${measurement.measurementKind.label})"
            } else {
                measurement.vitalSignType.displayName
            }

        return "$severity: $direction $label - ${measurement.value}"
    }

    private fun getPatientName(
        tei: TrackedEntityInstance,
        attributesByUid: Map<String, TrackedEntityAttribute>,
    ): String {
        val attributeValues = tei.trackedEntityAttributeValues().orEmpty()
        if (attributeValues.isEmpty()) return "Patient ${tei.uid().take(8)}"

        // Strategy 1: look for attributes whose name contains "first" / "given" / "last" / "surname"
        val firstName = findAttributeValue(tei, attributesByUid, listOf("first", "given"))
        val lastName  = findAttributeValue(tei, attributesByUid, listOf("last", "family", "surname"))
        if (!firstName.isNullOrBlank() || !lastName.isNullOrBlank()) {
            return listOfNotNull(firstName, lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }

        // Strategy 2: look for any attribute whose name contains "name"
        val nameValue = findAttributeValue(tei, attributesByUid, listOf("name"))
        if (!nameValue.isNullOrBlank()) return nameValue

        // Strategy 3: concatenate the first two non-blank, non-numeric attribute values
        // (covers programs where attributes are labelled differently, e.g. "Patient First Name",
        //  "Prénom", "Jina la kwanza", etc.)
        val allTextValues = attributeValues
            .mapNotNull { av ->
                val value = av.value()?.trim() ?: return@mapNotNull null
                // Skip values that look like IDs, dates, or pure numbers
                if (value.isBlank()) return@mapNotNull null
                if (value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return@mapNotNull null
                if (value.matches(Regex("[0-9]+"))) return@mapNotNull null
                if (value.length <= 2) return@mapNotNull null
                value
            }
            .take(2)

        if (allTextValues.isNotEmpty()) return allTextValues.joinToString(" ")

        // Final fallback: short UID
        return "Patient ${tei.uid().take(8)}"
    }

    private fun buildFallbackPatientName(event: Event): String {
        val orgUnitName =
            event.organisationUnit()
                ?.let { orgUnitUid ->
                    d2.organisationUnitModule().organisationUnits().uid(orgUnitUid).blockingGet()?.displayName()
                }

        return orgUnitName?.takeIf { it.isNotBlank() }
            ?.let { "Patient - $it" }
            ?: "Patient ${event.uid().take(8)}"
    }

    private fun getPatientAge(
        tei: TrackedEntityInstance,
        attributesByUid: Map<String, TrackedEntityAttribute>,
    ): Int? {
        findAttributeValue(tei, attributesByUid, listOf("age"))?.toIntOrNull()?.let { return it }

        val dateOfBirth = findAttributeValue(tei, attributesByUid, listOf("birth", "dob")) ?: return null
        return try {
            Period.between(LocalDate.parse(dateOfBirth), LocalDate.now()).years
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun getPatientGender(
        tei: TrackedEntityInstance,
        attributesByUid: Map<String, TrackedEntityAttribute>,
    ): String? =
        findAttributeValue(
            tei = tei,
            attributesByUid = attributesByUid,
            keywords = listOf("gender", "sex"),
        )

    private fun findAttributeValue(
        tei: TrackedEntityInstance,
        attributesByUid: Map<String, TrackedEntityAttribute>,
        keywords: List<String>,
    ): String? =
        tei.trackedEntityAttributeValues()
            .orEmpty()
            .firstNotNullOfOrNull { attributeValue ->
                val attributeUid = attributeValue.trackedEntityAttribute() ?: return@firstNotNullOfOrNull null
                val attribute = attributesByUid[attributeUid]
                val searchableText =
                    buildString {
                        append(attribute?.displayName().orEmpty())
                        append(' ')
                        append(attribute?.name().orEmpty())
                        append(' ')
                        append(attribute?.code().orEmpty())
                    }.lowercase()

                if (keywords.any { keyword -> searchableText.contains(keyword) }) {
                    attributeValue.value()
                } else {
                    null
                }
            }

    private fun resolveTrackedEntityInstance(event: Event): String? =
        event.enrollment()
            ?.let { enrollmentUid -> d2.enrollmentModule().enrollments().uid(enrollmentUid).blockingGet() }
            ?.trackedEntityInstance()

    private fun syntheticPatientUid(event: Event): String =
        event.enrollment()
            ?: event.uid()

    private fun resolveVitalSignDefinition(
        dataElementUid: String?,
        dataElementsByUid: Map<String, DataElement>,
    ): VitalSignDefinition? {
        vitalSignConfig.getVitalSignDefinition(dataElementUid)?.let { return it }

        val dataElement = dataElementUid?.let { dataElementsByUid[it] } ?: return null
        val searchableText =
            buildString {
                append(dataElement.displayFormName().orEmpty())
                append(' ')
                append(dataElement.displayName().orEmpty())
                append(' ')
                append(dataElement.name().orEmpty())
                append(' ')
                append(dataElement.code().orEmpty())
            }

        val measurementKind = inferMeasurementKind(searchableText) ?: return null
        return VitalSignDefinition(
            vitalSignType = measurementKind.vitalSignType,
            measurementKind = measurementKind,
            unit = defaultUnitFor(measurementKind),
        )
    }

    private fun inferMeasurementKind(searchableText: String): VitalMeasurementKind? {
        val normalized = searchableText.lowercase()
        return when {
            "systolic" in normalized -> VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC
            "diastolic" in normalized -> VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC
            "spo2" in normalized || "oxygen saturation" in normalized || "oxygen sat" in normalized ->
                VitalMeasurementKind.SPO2
            "temperature" in normalized || "temp" in normalized ->
                VitalMeasurementKind.TEMPERATURE
            "pulse" in normalized || "heart rate" in normalized || "bpm" in normalized ->
                VitalMeasurementKind.PULSE_RATE
            "blood glucose" in normalized || "glucose" in normalized || "sugar" in normalized ->
                VitalMeasurementKind.BLOOD_GLUCOSE
            "respiratory" in normalized || "respiration" in normalized || "breath" in normalized ->
                VitalMeasurementKind.RESPIRATORY_RATE
            "weight" in normalized -> VitalMeasurementKind.WEIGHT
            "height" in normalized -> VitalMeasurementKind.HEIGHT
            "blood pressure" in normalized -> VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC
            else -> null
        }
    }

    private fun defaultUnitFor(measurementKind: VitalMeasurementKind): String =
        when (measurementKind) {
            VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC,
            VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC,
            -> "mmHg"
            VitalMeasurementKind.TEMPERATURE -> "C"
            VitalMeasurementKind.PULSE_RATE -> "bpm"
            VitalMeasurementKind.SPO2 -> "%"
            VitalMeasurementKind.BLOOD_GLUCOSE -> "mg/dL"
            VitalMeasurementKind.RESPIRATORY_RATE -> "breaths/min"
            VitalMeasurementKind.WEIGHT -> "kg"
            VitalMeasurementKind.HEIGHT -> "cm"
        }

    private fun normalRangeFor(measurementKind: VitalMeasurementKind): Pair<Float, Float>? =
        when (measurementKind) {
            VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC -> 90f to 140f
            VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC -> 60f to 90f
            VitalMeasurementKind.TEMPERATURE -> 36.1f to 37.2f
            VitalMeasurementKind.PULSE_RATE -> 60f to 100f
            VitalMeasurementKind.SPO2 -> 95f to 100f
            VitalMeasurementKind.BLOOD_GLUCOSE -> 70f to 140f
            VitalMeasurementKind.RESPIRATORY_RATE -> 12f to 20f
            VitalMeasurementKind.WEIGHT,
            VitalMeasurementKind.HEIGHT,
            -> null
        }

    private fun criticalRangeFor(measurementKind: VitalMeasurementKind): Pair<Float, Float>? =
        when (measurementKind) {
            VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC -> 70f to 180f
            VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC -> 40f to 120f
            VitalMeasurementKind.TEMPERATURE -> 35f to 40f
            VitalMeasurementKind.PULSE_RATE -> 40f to 150f
            VitalMeasurementKind.SPO2 -> 85f to 100f
            VitalMeasurementKind.BLOOD_GLUCOSE -> 40f to 300f
            VitalMeasurementKind.RESPIRATORY_RATE -> 8f to 30f
            VitalMeasurementKind.WEIGHT,
            VitalMeasurementKind.HEIGHT,
            -> null
        }

    private companion object {
        val bloodPressureKinds =
            setOf(
                VitalMeasurementKind.BLOOD_PRESSURE_SYSTOLIC,
                VitalMeasurementKind.BLOOD_PRESSURE_DIASTOLIC,
            )
    }
}
