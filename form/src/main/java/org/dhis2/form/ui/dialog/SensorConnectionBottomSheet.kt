package org.dhis2.form.ui.dialog

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dhis2.form.R
import org.dhis2.form.ui.FormViewModel
import org.hisp.dhis.mobile.ui.designsystem.component.BottomSheetShell
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicator
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicatorType
import org.hisp.dhis.mobile.ui.designsystem.component.state.BottomSheetShellUIState
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing

const val SENSOR_DIALOG_TAG = "SensorConnectionBottomSheet"

/** Permissions required for BLE scanning. Android 12+ needs BLUETOOTH_SCAN/CONNECT;
 *  older versions need ACCESS_FINE_LOCATION. */
private val BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
} else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
}

class SensorConnectionBottomSheet(
    private val fieldUid: String,
    private val viewModel: FormViewModel,
    /** For multi-value sensors (oximeter): UID of the second field to fill. */
    private val secondaryFieldUid: String? = null,
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        Log.d("SensorBottomSheet", "Opening Bluetooth sensor dialog for field: $fieldUid")
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            DHIS2Theme {
                SensorConnectionScreen(
                    fieldUid = fieldUid,
                    secondaryFieldUid = secondaryFieldUid,
                    viewModel = viewModel,
                    onDismiss = {
                        Log.d("SensorBottomSheet", "Closing dialog")
                        dismissAllowingStateLoss()
                    },
                )
            }
        }
    }

    fun show(manager: FragmentManager) {
        super.show(manager, SENSOR_DIALOG_TAG)
    }
}

@Composable
fun SensorConnectionScreen(
    fieldUid: String,
    secondaryFieldUid: String? = null,
    viewModel: FormViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isScanningMap by viewModel.isFieldScanning.collectAsState()
    val isScanning = isScanningMap[fieldUid] ?: false
    val sensorStatuses by viewModel.sensorStatuses.collectAsState()
    val status = sensorStatuses[fieldUid] ?: "Initializing..."

    var permissionDenied by remember { mutableStateOf(false) }
    var scanStartTime by remember { mutableStateOf(0L) }

    // Request BLE permissions then start scan
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            Log.d("SensorBottomSheet", "BLE permissions granted — starting scan")
            scanStartTime = System.currentTimeMillis()
            viewModel.startSensorScan(fieldUid, secondaryFieldUid)
        } else {
            Log.w("SensorBottomSheet", "BLE permissions denied")
            permissionDenied = true
        }
    }

    // Check permissions and start scan immediately when dialog opens
    LaunchedEffect(Unit) {
        Log.d("SensorBottomSheet", "Auto-starting BLE scan for field: $fieldUid")
        val allGranted = BLE_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            scanStartTime = System.currentTimeMillis()
            viewModel.startSensorScan(fieldUid, secondaryFieldUid)
        } else {
            permissionLauncher.launch(BLE_PERMISSIONS)
        }
    }

    // Show timeout message after 30 seconds
    LaunchedEffect(isScanning) {
        if (isScanning && scanStartTime > 0) {
            delay(30000) // 30 seconds
            if (isScanning) {
                Log.w("SensorBottomSheet", "Scan timeout after 30 seconds")
            }
        }
    }

    // Auto-dismiss once data is received on either field
    val secondaryStatuses by viewModel.sensorStatuses.collectAsState()
    val secondaryStatus = if (secondaryFieldUid != null) secondaryStatuses[secondaryFieldUid] ?: "" else ""

    LaunchedEffect(status, secondaryStatus) {
        if (status.startsWith("Data received") ||
            (secondaryFieldUid != null && secondaryStatus.startsWith("Data received"))
        ) {
            Log.d("SensorBottomSheet", "Data received — dismissing")
            delay(1200)
            onDismiss()
        }
    }

    BottomSheetShell(
        uiState = BottomSheetShellUIState(
            title = "Bluetooth Sensor",
            showTopSectionDivider = true,
        ),
        onDismiss = {
            coroutineScope.launch {
                viewModel.stopSensorScan()
                delay(100)
                onDismiss()
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Spacing16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    permissionDenied -> BluetoothSensorStatus(
                        status = "Bluetooth permission denied.\nPlease grant it in Settings.",
                        isSuccess = false,
                    )
                    status.startsWith("Data received") -> BluetoothSensorStatus(
                        status = status,
                        isSuccess = true,
                    )
                    status == "No connections available" -> BluetoothSensorStatus(
                        status = "No sensor found. Try again.",
                        isSuccess = false,
                    )
                    status.contains("Place finger", ignoreCase = true) -> BluetoothSensorPlaceFinger(status)
                    status.contains("Connecting", ignoreCase = true) -> BluetoothSensorConnecting(status)
                    status.contains("Scanning", ignoreCase = true) || 
                    status.contains("Waiting", ignoreCase = true) ||
                    status.contains("Initializing", ignoreCase = true) -> BluetoothSensorSearching(status)
                    else -> BluetoothSensorSearching(status)
                }
            }
        },
        buttonBlock = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                text = "CANCEL",
                onClick = {
                    coroutineScope.launch {
                        viewModel.stopSensorScan()
                        delay(100)
                        onDismiss()
                    }
                },
            )
        },
    )
}

@Composable
private fun BluetoothSensorSearching(statusMessage: String = "Waiting for sensor...") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_bluetooth),
            contentDescription = null,
            modifier = Modifier.size(Spacing.Spacing48),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing8))
        Text(
            text = "Bluetooth Sensor",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing8))
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing16))
        ProgressIndicator(type = ProgressIndicatorType.CIRCULAR_SMALL)
    }
}

@Composable
private fun BluetoothSensorConnecting(statusMessage: String = "Connecting...") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = statusMessage,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing16))
        ProgressIndicator(type = ProgressIndicatorType.CIRCULAR_SMALL)
    }
}

@Composable
private fun BluetoothSensorPlaceFinger(statusMessage: String = "Place finger on sensor") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_bluetooth),
            contentDescription = null,
            modifier = Modifier.size(Spacing.Spacing48),
            tint = Color(0xFF4CAF50), // Green to indicate ready
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing8))
        Text(
            text = statusMessage,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF4CAF50),
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing8))
        Text(
            text = "Waiting for reading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing16))
        ProgressIndicator(type = ProgressIndicatorType.CIRCULAR_SMALL)
    }
}

@Composable
private fun BluetoothSensorStatus(status: String, isSuccess: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceRow(device: BluetoothDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.Spacing8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_bluetooth),
            contentDescription = null,
            modifier = Modifier.size(Spacing.Spacing24),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(Spacing.Spacing16))
        Column {
            Text(text = device.name ?: "Unknown Device", style = MaterialTheme.typography.bodyLarge)
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
        }
    }
}
