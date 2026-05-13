package org.dhis2.usescases.vitaldashboard.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.usescases.vitaldashboard.*
import org.dhis2.usescases.vitaldashboard.model.VitalSignType
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import timber.log.Timber
import java.util.*

/**
 * Repository for Vital Signs Dashboard
 * 
 * Fetches and processes vital signs data from DHIS2 SDK with real-time monitoring capabilities.
 * Handles:
 * - Real-time event data retrieval
 * - Tracked entity data
 * - Data value aggregation
 * - Alert detection
 * - Trend calculation
 * - Live dashboard updates
 * 
 * @property d2 DHIS2 SDK instance
 * @property dispatchers Coroutine dispatchers
 * @property vitalSignConfig Configuration for vital sign data elements
 * 
 * @author Shadreck Mkandawire
 */
class VitalDashboardRepository(
    private val d2: D2,
    private val dispatchers: Dispatcher,
    private val vitalSignConfig: VitalSignConfig
) {

    companion object {
        private const val REAL_TIME_REFRESH_INTERVAL_MS = 30000L // 30 seconds
        private const val TAG = "VitalDashboardRepo"
    }

    /**
     * Check if current user has authorization to access dashboard
     * Restricted to: Doctors, Clinicians, Administrators
     */
    suspend fun isUserAuthorized(): Boolean = withContext(dispatchers.io) {
        try {
            val user = d2.userModule().user().blockingGet()
            // Extract role UIDs directly to avoid UidsHelper type inference issues
            val roleUids = user?.userRoles()?.mapNotNull { it.uid() } ?: emptyList()
            if (roleUids.isEmpty()) return@withContext false

            val userRoles = d2.userModule().userRoles()
                .byUid().`in`(roleUids)
                .blockingGet()

            val authorizedRoleNames = setOf(
                "Doctor",
                "Clinician",
                "Administrator",
                "Admin",
                "Physician",
                "Nurse",
                "Healthcare Worker"
            )

            val hasAuthorizedRole = userRoles.any { role ->
                authorizedRoleNames.any { authorizedName ->
                    role.name()?.contains(authorizedName, ignoreCase = true) == true
                }
            }

            Timber.tag(TAG).d("User authorization check: $hasAuthorizedRole (roles: ${userRoles.map { it.name() }})")
            hasAuthorizedRole
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error checking user authorization")
            false
        }
    }

    /**
     * Get dashboard data with optional filtering (one-time fetch)
     */
    suspend fun getDashboardData(filter: VitalDashboardFilter): VitalDashboardData = 
        withContext(dispatchers.io) {
            try {
                Timber.tag(TAG).d("Loading dashboard data with filter: $filter")

                // Fetch events containing vital signs data
                val events = fetchVitalSignEvents(filter)
                Timber.tag(TAG).d("Fetched ${events.size} vital sign events")

                // Fetch tracked entity instances (patients)
                val patients = fetchPatients(events)
                Timber.tag(TAG).d("Fetched ${patients.size} patients")

                // Process events into measurements
                val measurements = processEventsToMeasurements(events, patients)
                Timber.tag(TAG).d("Processed ${measurements.size} measurements")

                // Generate patient summaries
                val patientSummaries = generatePatientSummaries(measurements, patients)
                Timber.tag(TAG).d("Generated ${patientSummaries.size} patient summaries")

                // Detect alerts
                val alerts = detectAlerts(measurements)
                Timber.tag(TAG).d("Detected ${alerts.size} alerts")

                // Calculate statistics
                val statistics = calculateStatistics(measurements, patients.size)

                // Generate trends
                val trends = generateTrends(measurements)

                // Filter recent measurements
                val recentMeasurements = measurements
                    .sortedByDescending { it.timestamp }
                    .take(50)

                VitalDashboardData(
                    patientSummaries = patientSummaries,
                    recentMeasurements = recentMeasurements,
                    alerts = alerts,
                    statistics = statistics,
                    trends = trends
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading dashboard data")
                throw e
            }
        }

    /**
     * Observe dashboard data in real-time with automatic refresh
     * 
     * This Flow emits updated dashboard data at regular intervals,
     * allowing the UI to display live updates as new vital signs are captured.
     * 
     * @param filter Dashboard filter criteria
     * @param refreshIntervalMs Refresh interval in milliseconds (default: 30 seconds)
     * @return Flow of VitalDashboardData that updates automatically
     */
    fun observeDashboardData(
        filter: VitalDashboardFilter,
        refreshIntervalMs: Long = REAL_TIME_REFRESH_INTERVAL_MS
    ): Flow<VitalDashboardData> = flow {
        Timber.tag(TAG).d("Starting real-time dashboard observation (refresh every ${refreshIntervalMs}ms)")
        
        while (true) {
            try {
                val dashboardData = getDashboardData(filter)
                emit(dashboardData)
                Timber.tag(TAG).d("Emitted real-time dashboard update: ${dashboardData.patientSummaries.size} patients, ${dashboardData.alerts.size} alerts")
                delay(refreshIntervalMs)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error in real-time dashboard observation")
                // Continue observing even if one fetch fails
                delay(refreshIntervalMs)
            }
        }
    }

    /**
     * Observe vital sign statistics in real-time
     * 
     * Provides a lightweight stream of statistics without full dashboard data.
     * Useful for summary cards and quick metrics.
     * 
     * @param filter Dashboard filter criteria
     * @param refreshIntervalMs Refresh interval in milliseconds
     * @return Flow of VitalStatistics
     */
    fun observeStatistics(
        filter: VitalDashboardFilter,
        refreshIntervalMs: Long = REAL_TIME_REFRESH_INTERVAL_MS
    ): Flow<VitalStatistics> = flow {
        Timber.tag(TAG).d("Starting real-time statistics observation")
        
        while (true) {
            try {
                val events = fetchVitalSignEvents(filter)
                val patients = fetchPatients(events)
                val measurements = processEventsToMeasurements(events, patients)
                val statistics = calculateStatistics(measurements, patients.size)
                
                emit(statistics)
                Timber.tag(TAG).d("Emitted statistics update: ${statistics.totalPatients} patients, ${statistics.activeAlerts} alerts")
                delay(refreshIntervalMs)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error in real-time statistics observation")
                delay(refreshIntervalMs)
            }
        }
    }

    /**
     * Observe alerts in real-time
     * 
     * Monitors for new abnormal vital signs and emits alerts as they occur.
     * Critical for clinical monitoring and immediate response.
     * 
     * @param filter Dashboard filter criteria
     * @param refreshIntervalMs Refresh interval in milliseconds
     * @return Flow of alert lists
     */
    fun observeAlerts(
        filter: VitalDashboardFilter,
        refreshIntervalMs: Long = REAL_TIME_REFRESH_INTERVAL_MS
    ): Flow<List<VitalAlert>> = flow {
        Timber.tag(TAG).d("Starting real-time alerts observation")
        
        while (true) {
            try {
                val events = fetchVitalSignEvents(filter)
                val patients = fetchPatients(events)
                val measurements = processEventsToMeasurements(events, patients)
                val alerts = detectAlerts(measurements)
                
                emit(alerts)
                Timber.tag(TAG).d("Emitted alerts update: ${alerts.size} alerts (${alerts.count { it.alertType == AlertType.CRITICAL_HIGH || it.alertType == AlertType.CRITICAL_LOW }} critical)")
                delay(refreshIntervalMs)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error in real-time alerts observation")
                delay(refreshIntervalMs)
            }
        }
    }

    /**
     * Fetch vital sign events from DHIS2
     */
    private fun fetchVitalSignEvents(filter: VitalDashboardFilter): List<Event> {
        val eventQuery = d2.eventModule().events()
            .byDeleted().isFalse

        // Apply date filter
        filter.startDate?.let { start ->
            eventQuery.byEventDate().afterOrEqual(Date(start))
        }
        filter.endDate?.let { end ->
            eventQuery.byEventDate().beforeOrEqual(Date(end))
        }

        // Fetch events
        val events = eventQuery.blockingGet()

        // Filter events that contain vital sign data elements
        return events.filter { event ->
            event.trackedEntityDataValues()?.any { dataValue ->
                vitalSignConfig.isVitalSignDataElement(dataValue.dataElement())
            } == true
        }
    }

    /**
     * Fetch patient (tracked entity) data by resolving enrollment → TEI chain.
     * Event.trackedEntityInstance() is package-private in this SDK version,
     * so we go through the enrollment instead.
     */
    private fun fetchPatients(events: List<Event>): Map<String, TrackedEntityInstance> {
        val enrollmentUids = events.mapNotNull { it.enrollment() }.distinct()
        if (enrollmentUids.isEmpty()) return emptyMap()

        val teiUids = d2.enrollmentModule().enrollments()
            .byUid().`in`(enrollmentUids)
            .blockingGet()
            .mapNotNull { it.trackedEntityInstance() }
            .distinct()

        if (teiUids.isEmpty()) return emptyMap()

        val teis = d2.trackedEntityModule().trackedEntityInstances()
            .byUid().`in`(teiUids)
            .blockingGet()

        return teis.associateBy { it.uid() }
    }

    /** Maps event UID → TEI UID via enrollment, cached for the current batch. */
    private fun buildEventToTeiMap(events: List<Event>): Map<String, String> {
        val enrollmentUids = events.mapNotNull { it.enrollment() }.distinct()
        if (enrollmentUids.isEmpty()) return emptyMap()

        return d2.enrollmentModule().enrollments()
            .byUid().`in`(enrollmentUids)
            .blockingGet()
            .mapNotNull { enrollment ->
                val teiUid = enrollment.trackedEntityInstance() ?: return@mapNotNull null
                enrollment.uid() to teiUid
            }
            .toMap()
    }

    /**
     * Process events into vital measurements
     */
    private fun processEventsToMeasurements(
        events: List<Event>,
        patients: Map<String, TrackedEntityInstance>
    ): List<VitalMeasurement> {
        val measurements = mutableListOf<VitalMeasurement>()
        val eventToTei = buildEventToTeiMap(events)

        events.forEach { event ->
            val enrollmentUid = event.enrollment() ?: return@forEach
            val patientUid = eventToTei[enrollmentUid] ?: return@forEach
            val patient = patients[patientUid] ?: return@forEach
            val patientName = getPatientName(patient)

            event.trackedEntityDataValues()?.forEach { dataValue ->
                val vitalSignType = vitalSignConfig.getVitalSignType(dataValue.dataElement())
                if (vitalSignType != null) {
                    val value = parseVitalValue(dataValue.value(), vitalSignType)
                    val isAbnormal = isValueAbnormal(value, vitalSignType)

                    measurements.add(
                        VitalMeasurement(
                            uid = "${event.uid()}_${dataValue.dataElement()}",
                            patientUid = patientUid,
                            patientName = patientName,
                            vitalSignType = vitalSignType,
                            value = value,
                            timestamp = event.eventDate()?.time ?: System.currentTimeMillis(),
                            isAbnormal = isAbnormal,
                            notes = null
                        )
                    )
                }
            }
        }

        return measurements
    }

    /**
     * Generate patient summaries with latest vitals
     */
    private fun generatePatientSummaries(
        measurements: List<VitalMeasurement>,
        patients: Map<String, TrackedEntityInstance>
    ): List<PatientVitalSummary> {
        val measurementsByPatient = measurements.groupBy { it.patientUid }

        return measurementsByPatient.map { (patientUid, patientMeasurements) ->
            val patient = patients[patientUid]
            val latestByType = patientMeasurements
                .groupBy { it.vitalSignType }
                .mapValues { (_, measurements) ->
                    measurements.maxByOrNull { it.timestamp }?.value
                }
                .filterValues { it != null }
                .mapValues { it.value!! }

            val hasAlerts = patientMeasurements.any { it.isAbnormal }
            val lastMeasurementTime = patientMeasurements.maxOfOrNull { it.timestamp } 
                ?: System.currentTimeMillis()

            PatientVitalSummary(
                patientUid = patientUid,
                patientName = patient?.let { getPatientName(it) } ?: "Unknown Patient",
                age = getPatientAge(patient),
                gender = getPatientGender(patient),
                latestVitals = latestByType,
                hasAlerts = hasAlerts,
                lastMeasurementTime = lastMeasurementTime
            )
        }.sortedByDescending { it.lastMeasurementTime }
    }

    /**
     * Detect abnormal vital signs and generate alerts
     */
    private fun detectAlerts(measurements: List<VitalMeasurement>): List<VitalAlert> {
        return measurements
            .filter { it.isAbnormal }
            .map { measurement ->
                val alertType = determineAlertType(measurement.value, measurement.vitalSignType)
                val message = generateAlertMessage(measurement.vitalSignType, alertType, measurement.value)

                VitalAlert(
                    uid = measurement.uid,
                    patientUid = measurement.patientUid,
                    patientName = measurement.patientName,
                    vitalSignType = measurement.vitalSignType,
                    alertType = alertType,
                    value = measurement.value,
                    timestamp = measurement.timestamp,
                    message = message
                )
            }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Calculate dashboard statistics
     */
    private fun calculateStatistics(
        measurements: List<VitalMeasurement>,
        totalPatients: Int
    ): VitalStatistics {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)

        val measurementsToday = measurements.count { it.timestamp >= oneDayAgo }
        val activeAlerts = measurements.count { it.isAbnormal && it.timestamp >= oneDayAgo }

        val averagesByType = measurements
            .groupBy { it.vitalSignType }
            .mapValues { (_, typeMeasurements) ->
                typeMeasurements
                    .mapNotNull { it.value.numericValue?.toDouble() }
                    .average()
            }
            .filterValues { !it.isNaN() }

        return VitalStatistics(
            totalPatients = totalPatients,
            totalMeasurements = measurements.size,
            activeAlerts = activeAlerts,
            measurementsToday = measurementsToday,
            averagesByType = averagesByType
        )
    }

    /**
     * Generate trend data for charts
     */
    private fun generateTrends(measurements: List<VitalMeasurement>): Map<VitalSignType, List<TrendDataPoint>> {
        return measurements
            .groupBy { it.vitalSignType }
            .mapValues { (_, typeMeasurements) ->
                typeMeasurements
                    .sortedBy { it.timestamp }
                    .mapNotNull { measurement ->
                        measurement.value.numericValue?.let { value ->
                            TrendDataPoint(
                                timestamp = measurement.timestamp,
                                value = value,
                                isAbnormal = measurement.isAbnormal
                            )
                        }
                    }
            }
    }

    /**
     * Parse vital value from string
     */
    private fun parseVitalValue(valueString: String?, vitalSignType: VitalSignType): VitalValue {
        if (valueString.isNullOrBlank()) {
            return VitalValue("--", vitalSignType.unit, null)
        }

        return try {
            // Handle blood pressure format (e.g., "120/80")
            if (vitalSignType == VitalSignType.BLOOD_PRESSURE && valueString.contains("/")) {
                val parts = valueString.split("/")
                val systolic = parts[0].trim().toFloatOrNull()
                VitalValue(valueString, vitalSignType.unit, systolic)
            } else {
                val numericValue = valueString.toFloatOrNull()
                VitalValue(valueString, vitalSignType.unit, numericValue)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing vital value: $valueString")
            VitalValue(valueString, vitalSignType.unit, null)
        }
    }

    /**
     * Check if vital value is abnormal
     */
    private fun isValueAbnormal(value: VitalValue, vitalSignType: VitalSignType): Boolean {
        val numericValue = value.numericValue ?: return false
        val range = vitalSignType.normalRange ?: return false

        return numericValue < range.first || numericValue > range.second
    }

    /**
     * Determine alert type based on value and thresholds
     */
    private fun determineAlertType(value: VitalValue, vitalSignType: VitalSignType): AlertType {
        val numericValue = value.numericValue ?: return AlertType.ABNORMAL
        val range = vitalSignType.normalRange ?: return AlertType.ABNORMAL
        val criticalRange = vitalSignType.criticalRange

        return when {
            criticalRange != null && numericValue < criticalRange.first -> AlertType.CRITICAL_LOW
            criticalRange != null && numericValue > criticalRange.second -> AlertType.CRITICAL_HIGH
            numericValue < range.first -> AlertType.LOW
            numericValue > range.second -> AlertType.HIGH
            else -> AlertType.ABNORMAL
        }
    }

    /**
     * Generate alert message
     */
    private fun generateAlertMessage(
        vitalSignType: VitalSignType,
        alertType: AlertType,
        value: VitalValue
    ): String {
        val severity = when (alertType) {
            AlertType.CRITICAL_HIGH, AlertType.CRITICAL_LOW -> "Critical"
            AlertType.HIGH, AlertType.LOW -> "Warning"
            AlertType.ABNORMAL -> "Alert"
        }

        val direction = when (alertType) {
            AlertType.HIGH, AlertType.CRITICAL_HIGH -> "High"
            AlertType.LOW, AlertType.CRITICAL_LOW -> "Low"
            AlertType.ABNORMAL -> "Abnormal"
        }

        return "$severity: $direction ${vitalSignType.displayName} - $value"
    }

    /**
     * Get patient name from tracked entity
     */
    private fun getPatientName(tei: TrackedEntityInstance): String {
        val attributes = tei.trackedEntityAttributeValues()
        
        // Try to find first name and last name attributes
        val firstName = attributes?.find { 
            it.trackedEntityAttribute()?.contains("first", ignoreCase = true) == true ||
            it.trackedEntityAttribute()?.contains("given", ignoreCase = true) == true
        }?.value()
        
        val lastName = attributes?.find { 
            it.trackedEntityAttribute()?.contains("last", ignoreCase = true) == true ||
            it.trackedEntityAttribute()?.contains("family", ignoreCase = true) == true ||
            it.trackedEntityAttribute()?.contains("surname", ignoreCase = true) == true
        }?.value()

        return when {
            firstName != null && lastName != null -> "$firstName $lastName"
            firstName != null -> firstName
            lastName != null -> lastName
            else -> "Patient ${tei.uid().take(8)}"
        }
    }

    /**
     * Get patient age from tracked entity
     */
    private fun getPatientAge(tei: TrackedEntityInstance?): Int? {
        if (tei == null) return null

        val attributes = tei.trackedEntityAttributeValues()
        
        // Try to find age or date of birth attribute
        val ageValue = attributes?.find { 
            it.trackedEntityAttribute()?.contains("age", ignoreCase = true) == true
        }?.value()?.toIntOrNull()

        if (ageValue != null) return ageValue

        // Try to calculate from date of birth
        val dobValue = attributes?.find { 
            it.trackedEntityAttribute()?.contains("birth", ignoreCase = true) == true ||
            it.trackedEntityAttribute()?.contains("dob", ignoreCase = true) == true
        }?.value()

        // TODO: Calculate age from date of birth
        return null
    }

    /**
     * Get patient gender from tracked entity
     */
    private fun getPatientGender(tei: TrackedEntityInstance?): String? {
        if (tei == null) return null

        val attributes = tei.trackedEntityAttributeValues()
        
        return attributes?.find { 
            it.trackedEntityAttribute()?.contains("gender", ignoreCase = true) == true ||
            it.trackedEntityAttribute()?.contains("sex", ignoreCase = true) == true
        }?.value()
    }
}
