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
import org.dhis2.form.ui.sensor.SensorFieldResolver
import org.dhis2.form.ui.sensor.SensorStatusText
import org.hisp.dhis.mobile.ui.designsystem.component.BottomSheetShell
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicator
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicatorType
import org.hisp.dhis.mobile.ui.designsystem.component.state.BottomSheetShellUIState
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing

const val SENSOR_DIALOG_TAG = "SensorConnectionBottomSheet"

private val BLE_PERMISSIONS =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
    ): View =
        ComposeView(requireContext()).apply {
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
    val status = sensorStatuses[fieldUid] ?: SensorStatusText.WAITING_FOR_DATA
    val secondaryStatus = secondaryFieldUid?.let { sensorStatuses[it] }.orEmpty()
    val displayStatus =
        status.takeIf { SensorFieldResolver.hasCompletedReading(it) }
            ?: secondaryStatus.takeIf { SensorFieldResolver.hasCompletedReading(it) }
            ?: status
    val hasReading =
        SensorFieldResolver.hasCompletedReading(status) ||
            SensorFieldResolver.hasCompletedReading(secondaryStatus)

    var permissionDenied by remember { mutableStateOf(false) }
    var scanStartTime by remember { mutableStateOf(0L) }

    val closeSheet: () -> Unit = {
        coroutineScope.launch {
            viewModel.stopSensorScan()
            delay(100)
            onDismiss()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.all { it }) {
            Log.d("SensorBottomSheet", "BLE permissions granted - starting scan")
            scanStartTime = System.currentTimeMillis()
            viewModel.startSensorScan(fieldUid, secondaryFieldUid)
        } else {
            Log.w("SensorBottomSheet", "BLE permissions denied")
            permissionDenied = true
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        Log.d("SensorBottomSheet", "Auto-starting BLE scan for field: $fieldUid")
        val allGranted =
            BLE_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        if (allGranted) {
            scanStartTime = System.currentTimeMillis()
            viewModel.startSensorScan(fieldUid, secondaryFieldUid)
        } else {
            permissionLauncher.launch(BLE_PERMISSIONS)
        }
    }

    androidx.compose.runtime.LaunchedEffect(isScanning, scanStartTime) {
        if (isScanning && scanStartTime > 0) {
            delay(30_000)
            if (isScanning) {
                Log.w("SensorBottomSheet", "Scan timeout after 30 seconds")
            }
        }
    }

    BottomSheetShell(
        uiState =
            BottomSheetShellUIState(
                title = "Bluetooth Sensor",
                showTopSectionDivider = true,
            ),
        onDismiss = closeSheet,
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Spacing16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    permissionDenied ->
                        BluetoothSensorStatus(
                            status = "Bluetooth permission denied.\nPlease grant it in Settings.",
                            isSuccess = false,
                        )

                    hasReading ->
                        BluetoothSensorSuccess(
                            status = displayStatus,
                            helperText = SensorStatusText.RETAKE_HINT,
                        )

                    displayStatus == "No connections available" ||
                        displayStatus == SensorStatusText.NO_SENSOR_FOUND ->
                        BluetoothSensorStatus(
                            status = SensorStatusText.NO_SENSOR_FOUND,
                            isSuccess = false,
                        )

                    displayStatus.contains("connecting", ignoreCase = true) ->
                        BluetoothSensorConnecting(
                            statusMessage = displayStatus,
                            helperText = SensorStatusText.WAITING_FOR_DATA,
                        )

                    displayStatus.contains("connected", ignoreCase = true) ->
                        BluetoothSensorConnecting(
                            statusMessage = SensorStatusText.CONNECTED,
                            helperText = SensorStatusText.WAITING_FOR_DATA,
                        )

                    displayStatus.contains("search", ignoreCase = true) ||
                        displayStatus.contains("scan", ignoreCase = true) ||
                        displayStatus.contains("waiting", ignoreCase = true) ||
                        displayStatus.contains("initializing", ignoreCase = true) ->
                        BluetoothSensorSearching(displayStatus)

                    else ->
                        BluetoothSensorSearching(displayStatus)
                }
            }
        },
        buttonBlock = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.Spacing8),
            ) {
                if (hasReading) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        text = "RECONNECT DEVICE",
                        onClick = {
                            viewModel.reconnectSensorDevice(fieldUid, secondaryFieldUid)
                        },
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        text = "RETAKE MEASUREMENT",
                        onClick = {
                            viewModel.retakeSensorMeasurement(fieldUid, secondaryFieldUid)
                        },
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        text = "DONE",
                        onClick = closeSheet,
                    )
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        text = "CANCEL",
                        onClick = closeSheet,
                    )
                }
            }
        },
    )
}

@Composable
private fun BluetoothSensorSearching(statusMessage: String = SensorStatusText.SCANNING) {
    Column(
        modifier =
            Modifier
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
private fun BluetoothSensorConnecting(
    statusMessage: String,
    helperText: String,
) {
    Column(
        modifier =
            Modifier
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
        Spacer(modifier = Modifier.height(Spacing.Spacing8))
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing16))
        ProgressIndicator(type = ProgressIndicatorType.CIRCULAR_SMALL)
    }
}

@Composable
private fun BluetoothSensorSuccess(
    status: String,
    helperText: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4CAF50),
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing8))
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BluetoothSensorStatus(status: String, isSuccess: Boolean) {
    Column(
        modifier =
            Modifier
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
        modifier =
            Modifier
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
