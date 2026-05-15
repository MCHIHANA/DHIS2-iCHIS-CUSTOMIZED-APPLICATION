package org.dhis2.form.ui.sensor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



/**
 * Text button for sensor connection.
 * Replaces the icon with a "Connect Sensor" text button as requested by supervisor.
 */
@Composable
fun SensorConnectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = "Connect Sensor",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Compact inline sensor button for use in form fields.
 * Layout: positioned between value field and Cancel button.
 */
@Composable
fun InlineSensorConnectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SensorConnectButton(
        onClick = onClick,
        modifier = modifier
    )
}
