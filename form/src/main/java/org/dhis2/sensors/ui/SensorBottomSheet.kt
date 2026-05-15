package org.dhis2.sensors.ui

import androidx.compose.runtime.Composable

@Composable
fun SensorBottomSheet(
    state: SensorUiState,
    content: @Composable () -> Unit = {},
) {
    SensorStatusView(state = state)
    content()
}
