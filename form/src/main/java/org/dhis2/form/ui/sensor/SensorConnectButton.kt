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
    text: String = "Connect Sensor",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Compact inline sensor button for use in form fields.
 * Layout: positioned between value field and Cancel button.
 */
@Composable
fun InlineSensorConnectButton(
    text: String = "Connect Sensor",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SensorConnectButton(
        text = text,
        onClick = onClick,
        modifier = modifier.width(100.dp)
    )
}
