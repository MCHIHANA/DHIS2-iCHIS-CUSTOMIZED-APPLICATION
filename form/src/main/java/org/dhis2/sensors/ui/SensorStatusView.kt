package org.dhis2.sensors.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SensorStatusView(state: SensorUiState) {
    val text = when (state) {
        SensorUiState.Idle -> ""
        is SensorUiState.Status -> state.message
    }
    Text(text = text)
}
