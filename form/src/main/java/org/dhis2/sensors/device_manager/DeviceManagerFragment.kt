package org.dhis2.sensors.device_manager

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.dhis2.form.R
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme

private val BLE_PERMISSIONS =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

class DeviceManagerFragment : Fragment() {
    private val viewModel: DeviceManagerViewModel by viewModels {
        DeviceManagerViewModelFactory(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DHIS2Theme {
                    DeviceManagerScreen(
                        viewModel = viewModel,
                        onBack = { requireActivity().finish() },
                    )
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceManagerScreen(
    viewModel: DeviceManagerViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val pairingDeviceType by viewModel.pairingDeviceType.collectAsState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val currentDeviceAddress by viewModel.currentDeviceAddress.collectAsState()
    val isReceivingData by viewModel.isReceivingData.collectAsState()
    val lastFailure by viewModel.lastFailure.collectAsState()

    var permissionDenied by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.all { it }
        permissionDenied = !granted
        if (granted) {
            pendingAction?.invoke()
        }
        pendingAction = null
    }

    fun runWithBlePermissions(action: () -> Unit) {
        val allGranted =
            BLE_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        if (allGranted) {
            permissionDenied = false
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(BLE_PERMISSIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDevices()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Device Manager") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DeviceManagerInfoCard()
            }

            if (permissionDenied) {
                item {
                    StatusBanner(
                        message = "Bluetooth permission is required to pair or reconnect devices.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            lastFailure?.let { message ->
                item {
                    StatusBanner(
                        message = message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            items(
                listOf(
                    DeviceType.TEMPERATURE,
                    DeviceType.BLOOD_PRESSURE,
                    DeviceType.SPO2,
                    DeviceType.GLUCOSE,
                ),
                key = { it.name },
            ) { deviceType ->
                DeviceTypeSection(
                    deviceType = deviceType,
                    pairedDevices = pairedDevices.filter { it.deviceType == deviceType },
                    isPairing = pairingDeviceType == deviceType,
                    availableDevices = availableDevices,
                    currentDeviceAddress = currentDeviceAddress,
                    connectionState = connectionState,
                    isReceivingData = isReceivingData,
                    lastFailure = lastFailure,
                    onAddDevice = {
                        runWithBlePermissions { viewModel.startPairing(deviceType) }
                    },
                    onCancelPairing = viewModel::stopPairing,
                    onPairDevice = { device ->
                        runWithBlePermissions { viewModel.savePairedDevice(device) }
                    },
                    onConnect = { device ->
                        runWithBlePermissions { viewModel.connect(device) }
                    },
                    onDisconnect = {
                        runWithBlePermissions { viewModel.disconnect() }
                    },
                    onRemove = viewModel::removeDevice,
                )
            }

            item {
                FutureReadyCard()
            }
        }
    }
}

@Composable
private fun DeviceManagerInfoCard() {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Pair once, reconnect faster",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Saved devices are reused during measurements so the app can attempt a direct connection before falling back to the current scan flow.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    color: Color,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.12f),
            ),
    ) {
        Text(
            text = message,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun DeviceTypeSection(
    deviceType: DeviceType,
    pairedDevices: List<SensorDevice>,
    isPairing: Boolean,
    availableDevices: List<BluetoothDevice>,
    currentDeviceAddress: String?,
    connectionState: org.dhis2.sensor.ble.BleManager.ConnectionState,
    isReceivingData: Boolean,
    lastFailure: String?,
    onAddDevice: () -> Unit,
    onCancelPairing: () -> Unit,
    onPairDevice: (BluetoothDevice) -> Unit,
    onConnect: (SensorDevice) -> Unit,
    onDisconnect: () -> Unit,
    onRemove: (String) -> Unit,
) {
    val newDevices =
        availableDevices.filterNot { discoveredDevice ->
            pairedDevices.any { pairedDevice ->
                pairedDevice.macAddress.equals(discoveredDevice.address, ignoreCase = true)
            }
        }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = deviceType.sectionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (pairedDevices.isEmpty()) {
                Text(
                    text = "No paired devices saved yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                pairedDevices.forEach { device ->
                    SavedDeviceCard(
                        device = device,
                        isActive = currentDeviceAddress.equals(device.macAddress, ignoreCase = true),
                        connectionState = connectionState,
                        isReceivingData = isReceivingData,
                        lastFailure = lastFailure,
                        onConnect = { onConnect(device) },
                        onDisconnect = onDisconnect,
                        onRemove = { onRemove(device.macAddress) },
                    )
                }
            }

            Button(
                text = deviceType.addActionLabel,
                onClick = onAddDevice,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isPairing) {
                PairingPanel(
                    devices = newDevices,
                    onPairDevice = onPairDevice,
                    onCancel = onCancelPairing,
                )
            }
        }
    }
}

@Composable
private fun SavedDeviceCard(
    device: SensorDevice,
    isActive: Boolean,
    connectionState: org.dhis2.sensor.ble.BleManager.ConnectionState,
    isReceivingData: Boolean,
    lastFailure: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = device.deviceName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = device.macAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Status: ${statusText(isActive, connectionState, isReceivingData, lastFailure)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Last Connected: ${device.lastConnected.formatLastConnected()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isActive &&
                    connectionState != org.dhis2.sensor.ble.BleManager.ConnectionState.DISCONNECTED
                ) {
                    Button(
                        text = "Disconnect",
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Button(
                        text = "Connect",
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Remove")
                }
            }
        }
    }
}

@Composable
private fun PairingPanel(
    devices: List<BluetoothDevice>,
    onPairDevice: (BluetoothDevice) -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Scanning for nearby devices...",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (devices.isEmpty()) {
                    "Keep the sensor powered on. New devices will appear here as soon as they are discovered."
                } else {
                    "Tap a discovered device to save it for fast reconnect."
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            devices.forEach { device ->
                AvailableDeviceRow(
                    device = device,
                    onPair = { onPairDevice(device) },
                )
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Cancel Scan")
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun AvailableDeviceRow(
    device: BluetoothDevice,
    onPair: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_bluetooth),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name ?: "Unknown Device",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onPair) {
            Text(text = "Pair")
        }
    }
}

@Composable
private fun FutureReadyCard() {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Future-ready architecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "The storage and reconnect layer is designed so WiFi and USB medical devices can be added later without replacing the current BLE workflows.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun statusText(
    isActive: Boolean,
    connectionState: org.dhis2.sensor.ble.BleManager.ConnectionState,
    isReceivingData: Boolean,
    lastFailure: String?,
): String =
    when {
        !isActive -> "Disconnected"
        lastFailure != null -> "Failed"
        isReceivingData -> "Receiving Data"
        connectionState == org.dhis2.sensor.ble.BleManager.ConnectionState.CONNECTED -> "Connected"
        connectionState == org.dhis2.sensor.ble.BleManager.ConnectionState.CONNECTING -> "Connecting"
        connectionState == org.dhis2.sensor.ble.BleManager.ConnectionState.DISCONNECTING -> "Disconnecting"
        else -> "Disconnected"
    }

private fun Long.formatLastConnected(): String {
    if (this <= 0L) {
        return "Never"
    }
    return java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.MEDIUM,
        java.text.DateFormat.SHORT,
    ).format(java.util.Date(this))
}
