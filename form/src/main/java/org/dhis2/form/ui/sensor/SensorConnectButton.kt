package org.dhis2.form.ui.sensor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

/**
 * Text button for sensor connection.
 * Replaces the icon with a "Connect Sensor" text button as requested by supervisor.
 */
@Composable
fun SensorConnectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Bluetooth,
            contentDescription = "Connect Sensor",
            tint = MaterialTheme.colorScheme.primary
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
