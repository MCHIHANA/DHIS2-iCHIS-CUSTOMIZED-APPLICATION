package org.dhis2.usescases.vitaldashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.dhis2.mobile.commons.coroutine.Dispatcher
import org.dhis2.usescases.vitaldashboard.model.VitalSignType
import org.dhis2.usescases.vitaldashboard.repository.VitalDashboardRepository
import timber.log.Timber

/**
 * ViewModel for Vital Signs Dashboard with Real-Time Monitoring
 * 
 * Manages UI state and business logic for the vital signs monitoring dashboard.
 * Coordinates data loading, filtering, real-time updates, and user interactions.
 * 
 * Features:
 * - Real-time dashboard data observation
 * - Live statistics updates
 * - Continuous alert monitoring
 * - Automatic refresh on data changes
 * 
 * @property repository Repository for accessing vital signs data from DHIS2
 * @property dispatchers Coroutine dispatchers for background operations
 * 
 * @author Shadreck Mkandawire
 */
class VitalDashboardViewModel(
    private val repository: VitalDashboardRepository,
    private val dispatchers: Dispatcher
) : ViewModel() {

    companion object {
        private const val TAG = "VitalDashboardVM"
    }

    private val _uiState = MutableStateFlow<VitalDashboardUiState>(VitalDashboardUiState.Loading)
    val uiState: StateFlow<VitalDashboardUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(DashboardTab.OVERVIEW)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    private val _filterState = MutableStateFlow(VitalDashboardFilter())
    val filterState: StateFlow<VitalDashboardFilter> = _filterState.asStateFlow()

    private val _realTimeEnabled = MutableStateFlow(false)
    val realTimeEnabled: StateFlow<Boolean> = _realTimeEnabled.asStateFlow()

    private var realTimeObservationJob: Job? = null

    init {
        checkAuthorization()
    }

    /**
     * Check if current user is authorized to access the dashboard
     */
    private fun checkAuthorization() {
        viewModelScope.launch(dispatchers.io) {
            try {
                val isAuthorized = repository.isUserAuthorized()
                if (isAuthorized) {
                    loadDashboardData()
                } else {
                    _uiState.value = VitalDashboardUiState.Unauthorized
                    Timber.tag(TAG).w("User not authorized to access Vital Signs Dashboard")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error checking authorization")
                _uiState.value = VitalDashboardUiState.Error("Failed to verify access permissions")
            }
        }
    }

    /**
     * Load dashboard data from repository (one-time fetch)
     */
    fun loadDashboardData() {
        viewModelScope.launch(dispatchers.io) {
            try {
                _uiState.value = VitalDashboardUiState.Loading
                
                val filter = _filterState.value
                val dashboardData = repository.getDashboardData(filter)
                
                if (dashboardData.isEmpty()) {
                    _uiState.value = VitalDashboardUiState.Empty
                } else {
                    _uiState.value = VitalDashboardUiState.Success(dashboardData)
                }
                
                Timber.tag(TAG).d("Dashboard data loaded successfully: ${dashboardData.patientSummaries.size} patients")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading dashboard data")
                _uiState.value = VitalDashboardUiState.Error(
                    e.message ?: "Failed to load vital signs data"
                )
            }
        }
    }

    /**
     * Enable real-time monitoring
     * 
     * Starts continuous observation of dashboard data with automatic updates.
     * Dashboard will refresh every 30 seconds to show latest vital signs.
     */
    fun enableRealTimeMonitoring() {
        if (_realTimeEnabled.value) {
            Timber.tag(TAG).d("Real-time monitoring already enabled")
            return
        }

        Timber.tag(TAG).i("Enabling real-time vital signs monitoring")
        _realTimeEnabled.value = true

        realTimeObservationJob = viewModelScope.launch(dispatchers.io) {
            repository.observeDashboardData(_filterState.value)
                .catch { e ->
                    Timber.tag(TAG).e(e, "Error in real-time observation")
                    _uiState.value = VitalDashboardUiState.Error(
                        "Real-time monitoring error: ${e.message}"
                    )
                }
                .collect { dashboardData ->
                    if (dashboardData.isEmpty()) {
                        _uiState.value = VitalDashboardUiState.Empty
                    } else {
                        _uiState.value = VitalDashboardUiState.Success(dashboardData)
                    }
                    Timber.tag(TAG).d("Real-time update: ${dashboardData.patientSummaries.size} patients, ${dashboardData.alerts.size} alerts")
                }
        }
    }

    /**
     * Disable real-time monitoring
     * 
     * Stops continuous observation and returns to manual refresh mode.
     */
    fun disableRealTimeMonitoring() {
        if (!_realTimeEnabled.value) {
            Timber.tag(TAG).d("Real-time monitoring already disabled")
            return
        }

        Timber.tag(TAG).i("Disabling real-time vital signs monitoring")
        _realTimeEnabled.value = false
        realTimeObservationJob?.cancel()
        realTimeObservationJob = null
    }

    /**
     * Toggle real-time monitoring on/off
     */
    fun toggleRealTimeMonitoring() {
        if (_realTimeEnabled.value) {
            disableRealTimeMonitoring()
        } else {
            enableRealTimeMonitoring()
        }
    }

    /**
     * Refresh dashboard data manually
     */
    fun refreshData() {
        Timber.tag(TAG).d("Manual refresh requested")
        if (_realTimeEnabled.value) {
            // If real-time is enabled, just log - data will refresh automatically
            Timber.tag(TAG).d("Real-time monitoring active - data refreshes automatically")
        } else {
            loadDashboardData()
        }
    }

    /**
     * Select a dashboard tab
     */
    fun selectTab(tab: DashboardTab) {
        _selectedTab.value = tab
        Timber.d("Selected tab: ${tab.title}")
    }

    /**
     * Show filter dialog
     */
    fun showFilterDialog() {
        // TODO: Implement filter dialog
        Timber.d("Show filter dialog")
    }

    /**
     * Apply filter to dashboard
     */
    fun applyFilter(filter: VitalDashboardFilter) {
        _filterState.value = filter
        loadDashboardData()
        Timber.d("Filter applied: $filter")
    }

    /**
     * Navigate to patient details
     */
    fun navigateToPatientDetails(patientUid: String) {
        // TODO: Implement navigation to patient details
        Timber.d("Navigate to patient: $patientUid")
    }

    /**
     * Show vital sign details
     */
    fun showVitalSignDetails(vitalSignType: VitalSignType) {
        // TODO: Implement vital sign details dialog
        Timber.d("Show vital sign details: $vitalSignType")
    }

    /**
     * Filter by date range
     */
    fun filterByDateRange(startDate: Long, endDate: Long) {
        val newFilter = _filterState.value.copy(
            startDate = startDate,
            endDate = endDate
        )
        applyFilter(newFilter)
    }

    /**
     * Filter by vital sign type
     */
    fun filterByVitalSign(vitalSignType: VitalSignType?) {
        val newFilter = _filterState.value.copy(
            vitalSignType = vitalSignType
        )
        applyFilter(newFilter)
    }

    /**
     * Filter by alert status
     */
    fun filterByAlertStatus(showAlertsOnly: Boolean) {
        val newFilter = _filterState.value.copy(
            showAlertsOnly = showAlertsOnly
        )
        applyFilter(newFilter)
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        applyFilter(VitalDashboardFilter())
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up real-time observation when ViewModel is destroyed
        disableRealTimeMonitoring()
        Timber.tag(TAG).d("ViewModel cleared - real-time monitoring stopped")
    }
}

/**
 * UI State for Vital Dashboard
 */
sealed class VitalDashboardUiState {
    object Loading : VitalDashboardUiState()
    data class Success(val data: VitalDashboardData) : VitalDashboardUiState()
    data class Error(val message: String) : VitalDashboardUiState()
    object Unauthorized : VitalDashboardUiState()
    object Empty : VitalDashboardUiState()
}

/**
 * Dashboard data model
 */
data class VitalDashboardData(
    val patientSummaries: List<PatientVitalSummary>,
    val recentMeasurements: List<VitalMeasurement>,
    val alerts: List<VitalAlert>,
    val statistics: VitalStatistics,
    val trends: Map<VitalSignType, List<TrendDataPoint>>
) {
    fun isEmpty(): Boolean = patientSummaries.isEmpty() && recentMeasurements.isEmpty()
}

/**
 * Patient vital summary
 */
data class PatientVitalSummary(
    val patientUid: String,
    val patientName: String,
    val age: Int?,
    val gender: String?,
    val latestVitals: Map<VitalSignType, VitalValue>,
    val hasAlerts: Boolean,
    val lastMeasurementTime: Long
)

/**
 * Vital measurement
 */
data class VitalMeasurement(
    val uid: String,
    val patientUid: String,
    val patientName: String,
    val vitalSignType: VitalSignType,
    val value: VitalValue,
    val timestamp: Long,
    val isAbnormal: Boolean,
    val notes: String?
)

/**
 * Vital alert
 */
data class VitalAlert(
    val uid: String,
    val patientUid: String,
    val patientName: String,
    val vitalSignType: VitalSignType,
    val alertType: AlertType,
    val value: VitalValue,
    val timestamp: Long,
    val message: String
)

enum class AlertType {
    HIGH,
    LOW,
    CRITICAL_HIGH,
    CRITICAL_LOW,
    ABNORMAL
}

/**
 * Vital statistics
 */
data class VitalStatistics(
    val totalPatients: Int,
    val totalMeasurements: Int,
    val activeAlerts: Int,
    val measurementsToday: Int,
    val averagesByType: Map<VitalSignType, Double>
)

/**
 * Trend data point
 */
data class TrendDataPoint(
    val timestamp: Long,
    val value: Float,
    val isAbnormal: Boolean
)

/**
 * Vital value with unit
 */
data class VitalValue(
    val value: String,
    val unit: String,
    val numericValue: Float? = null
) {
    override fun toString(): String = "$value $unit"
}

/**
 * Dashboard filter
 */
data class VitalDashboardFilter(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val vitalSignType: VitalSignType? = null,
    val showAlertsOnly: Boolean = false,
    val patientUid: String? = null
)
