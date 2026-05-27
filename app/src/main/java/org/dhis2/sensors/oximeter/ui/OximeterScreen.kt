package org.dhis2.sensors.oximeter.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhis2.sensors.oximeter.data.ConnectionStatus
import org.dhis2.sensors.oximeter.data.OximeterState
import org.dhis2.sensors.oximeter.data.SubmissionStatus
import org.dhis2.sensors.oximeter.viewmodel.OximeterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main screen for the FORA O2 Pulse Oximeter feature.
 *
 * Displays:
 * - Connection status
 * - SpO2 and heart rate readings
 * - Submit button
 * - Device information
 * - Error messages
 *
 * @param viewModel The oximeter view model
 * @param onNavigateBack Callback for back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OximeterScreen(
    viewModel: OximeterViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startScan()
        }
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!viewModel.hasRequiredPermissions()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            permissionLauncher.launch(permissions)
        }
    }

    // Show error messages in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Confirmation dialog
    if (uiState.submissionStatus == SubmissionStatus.CONFIRMING) {
        ConfirmationDialog(
            reading = uiState.latestReading,
            onConfirm = { viewModel.confirmAndSubmit() },
            onDismiss = { viewModel.cancelSubmission() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FORA O2 Pulse Oximeter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            ConnectionStatusCard(
                state = uiState,
                onScanClick = { viewModel.startScan() },
                onDisconnectClick = { viewModel.disconnect() }
            )

            // Readings Card
            if (uiState.latestReading != null) {
                ReadingsCard(state = uiState)
            }

            // Submit Button
            SubmitButton(
                state = uiState,
                onClick = { viewModel.showConfirmationDialog() }
            )

            // Device Info Card
            if (uiState.deviceInfo != null) {
                DeviceInfoCard(state = uiState)
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    state: OximeterState,
    onScanClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getConnectionColor(state.connectionStatus)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = getConnectionIcon(state.connectionStatus),
                    contentDescription = "Connection status",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
                Column {
                    Text(
                        text = "Connection Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Text(
                        text = getConnectionText(state.connectionStatus),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            when (state.connectionStatus) {
                ConnectionStatus.IDLE, ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {
                    Button(onClick = onScanClick) {
                        Text("Connect")
                    }
                }
                ConnectionStatus.CONNECTED -> {
                    OutlinedButton(onClick = onDisconnectClick) {
                        Text("Disconnect", color = Color.White)
                    }
                }
                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingsCard(state: OximeterState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // SpO2 Reading
            ReadingDisplay(
                label = "SpO2",
                value = "${state.latestReading?.spo2}",
                unit = "%",
                icon = Icons.Default.Favorite,
                color = MaterialTheme.colorScheme.primary
            )

            // Heart Rate Reading
            ReadingDisplay(
                label = "Heart Rate",
                value = "${state.latestReading?.heartRateBpm}",
                unit = "BPM",
                icon = Icons.Default.Favorite,
                color = MaterialTheme.colorScheme.error
            )

            // Timestamp
            state.latestReading?.let { reading ->
                Text(
                    text = "Last updated: ${formatTimestamp(reading.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stability indicator
            if (state.hasStableReading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Stable",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Readings are stable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Stabilizing",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Waiting for stable readings...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingDisplay(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 56.sp
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun SubmitButton(
    state: OximeterState,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = state.canSubmit
    ) {
        when (state.submissionStatus) {
            SubmissionStatus.SUBMITTING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitting...")
            }
            SubmissionStatus.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submitted Successfully")
            }
            SubmissionStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submission Failed")
            }
            else -> {
                Text("Submit to DHIS2")
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(state: OximeterState) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.deviceInfo?.let { info ->
                        InfoRow("Manufacturer", info.manufacturer)
                        InfoRow("Model", info.model)
                        InfoRow("MAC Address", info.macAddress)
                        InfoRow("Firmware", info.firmwareVersion)
                        InfoRow("Serial Number", info.serialNumber)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ConfirmationDialog(
    reading: org.dhis2.sensors.oximeter.data.OximeterReading?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Confirmation"
            )
        },
        title = {
            Text("Confirm Submission")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to submit these readings to DHIS2?")
                Spacer(modifier = Modifier.height(8.dp))
                reading?.let {
                    Text("SpO2: ${it.spo2}%", fontWeight = FontWeight.Bold)
                    Text("Heart Rate: ${it.heartRateBpm} BPM", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions

private fun getConnectionColor(status: ConnectionStatus): Color {
    return when (status) {
        ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
        ConnectionStatus.SCANNING, ConnectionStatus.CONNECTING, ConnectionStatus.BONDING -> Color(0xFF2196F3)
        ConnectionStatus.DISCONNECTED -> Color(0xFF9E9E9E)
        ConnectionStatus.ERROR -> Color(0xFFF44336)
        ConnectionStatus.IDLE -> Color(0xFF757575)
    }
}

private fun getConnectionIcon(status: ConnectionStatus): ImageVector {
    return when (status) {
        ConnectionStatus.CONNECTED -> Icons.Default.BluetoothConnected
        ConnectionStatus.SCANNING, ConnectionStatus.CONNECTING, ConnectionStatus.BONDING -> Icons.Default.BluetoothSearching
        ConnectionStatus.DISCONNECTED, ConnectionStatus.IDLE -> Icons.Default.Bluetooth
        ConnectionStatus.ERROR -> Icons.Default.BluetoothDisabled
    }
}

private fun getConnectionText(status: ConnectionStatus): String {
    return when (status) {
        ConnectionStatus.IDLE -> "Not Connected"
        ConnectionStatus.SCANNING -> "Scanning..."
        ConnectionStatus.CONNECTING -> "Connecting..."
        ConnectionStatus.BONDING -> "Pairing..."
        ConnectionStatus.CONNECTED -> "Connected"
        ConnectionStatus.DISCONNECTED -> "Disconnected"
        ConnectionStatus.ERROR -> "Error"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
