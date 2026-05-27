package org.dhis2.sensors.oximeter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.dhis2.sensors.oximeter.ble.BleManager
import org.dhis2.sensors.oximeter.data.ConnectionStatus
import org.dhis2.sensors.oximeter.data.OximeterReading
import org.dhis2.sensors.oximeter.data.OximeterState
import org.dhis2.sensors.oximeter.data.SubmissionStatus
import org.dhis2.sensors.oximeter.dhis2.Dhis2Repository
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the oximeter feature.
 *
 * Manages:
 * - BLE connection state
 * - Oximeter readings
 * - DHIS2 submission
 * - UI state
 *
 * @property bleManager BLE manager for device communication
 * @property dhis2Repository Repository for DHIS2 operations
 */
class OximeterViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val dhis2Repository: Dhis2Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OximeterState())
    val uiState: StateFlow<OximeterState> = _uiState.asStateFlow()

    private var cachedOrgUnitUid: String? = null
    private var previousReading: OximeterReading? = null

    init {
        observeBleState()
    }

    /**
     * Observes BLE manager state flows and updates UI state accordingly.
     */
    private fun observeBleState() {
        viewModelScope.launch {
            combine(
                bleManager.connectionStatus,
                bleManager.latestReading,
                bleManager.deviceInfo,
                bleManager.errorMessage
            ) { connectionStatus, latestReading, deviceInfo, errorMessage ->
                // Update previous reading for stability check
                if (latestReading != null && latestReading != _uiState.value.latestReading) {
                    previousReading = _uiState.value.latestReading
                }

                _uiState.value = _uiState.value.copy(
                    connectionStatus = connectionStatus,
                    latestReading = latestReading,
                    previousReading = previousReading,
                    deviceInfo = deviceInfo,
                    errorMessage = errorMessage
                )
            }.collect { }
        }
    }

    /**
     * Checks if required BLE permissions are granted.
     */
    fun hasRequiredPermissions(): Boolean {
        return bleManager.hasRequiredPermissions()
    }

    /**
     * Starts scanning for the FORA O2 device.
     */
    fun startScan() {
        Timber.d("Starting scan")
        clearError()
        bleManager.startScan()
    }

    /**
     * Disconnects from the current device.
     */
    fun disconnect() {
        Timber.d("Disconnecting")
        bleManager.disconnect()
        previousReading = null
        _uiState.value = _uiState.value.copy(
            latestReading = null,
            previousReading = null,
            submissionStatus = SubmissionStatus.IDLE
        )
    }

    /**
     * Shows confirmation dialog for submission.
     */
    fun showConfirmationDialog() {
        if (_uiState.value.canSubmit) {
            _uiState.value = _uiState.value.copy(
                submissionStatus = SubmissionStatus.CONFIRMING
            )
        }
    }

    /**
     * Cancels the submission confirmation.
     */
    fun cancelSubmission() {
        _uiState.value = _uiState.value.copy(
            submissionStatus = SubmissionStatus.IDLE
        )
    }

    /**
     * Confirms and submits the current reading to DHIS2.
     */
    fun confirmAndSubmit() {
        val reading = _uiState.value.latestReading
        if (reading == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No reading available to submit",
                submissionStatus = SubmissionStatus.IDLE
            )
            return
        }

        if (!_uiState.value.hasStableReading) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Readings not stable. Please wait for consistent values.",
                submissionStatus = SubmissionStatus.IDLE
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            submissionStatus = SubmissionStatus.SUBMITTING,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                // Get org unit UID (cached or fetch)
                val orgUnitUid = cachedOrgUnitUid ?: run {
                    val uid = dhis2Repository.getCurrentUserOrgUnit()
                    cachedOrgUnitUid = uid
                    uid
                }

                // Submit readings
                val result = dhis2Repository.submitReadings(reading, orgUnitUid)

                result.fold(
                    onSuccess = { message ->
                        Timber.d("Submission successful: $message")
                        _uiState.value = _uiState.value.copy(
                            submissionStatus = SubmissionStatus.SUCCESS,
                            errorMessage = null
                        )
                        
                        // Reset to IDLE after 3 seconds
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(3000)
                            if (_uiState.value.submissionStatus == SubmissionStatus.SUCCESS) {
                                _uiState.value = _uiState.value.copy(
                                    submissionStatus = SubmissionStatus.IDLE
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Submission failed")
                        _uiState.value = _uiState.value.copy(
                            submissionStatus = SubmissionStatus.FAILED,
                            errorMessage = error.message ?: "Submission failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during submission")
                _uiState.value = _uiState.value.copy(
                    submissionStatus = SubmissionStatus.FAILED,
                    errorMessage = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
        
        // Also reset failed submission status
        if (_uiState.value.submissionStatus == SubmissionStatus.FAILED) {
            _uiState.value = _uiState.value.copy(
                submissionStatus = SubmissionStatus.IDLE
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.cleanup()
        Timber.d("ViewModel cleared")
    }
}
