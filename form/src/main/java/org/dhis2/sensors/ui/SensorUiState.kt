package org.dhis2.sensors.ui

sealed interface SensorUiState {
    data object Idle : SensorUiState
    data class Status(val message: String) : SensorUiState
}
