package org.dhis2.form.ui.dialog

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

class SensorConnectionBottomSheet(
    private val fieldUid: String,
    private val viewModel: FormViewModel,
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
    viewModel: FormViewModel,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val isScanningMap by viewModel.isFieldScanning.collectAsState()
    val isScanning = isScanningMap[fieldUid] ?: false
    val sensorStatuses by viewModel.sensorStatuses.collectAsState()
    val status = sensorStatuses[fieldUid] ?: ""

    // Start scan immediately when dialog opens — no tap required, no intent pipeline race
    LaunchedEffect(Unit) {
        Log.d("SensorBottomSheet", "Auto-starting BLE scan for field: $fieldUid")
        // Set scanning state in ViewModel first so the UI shows the spinner
        viewModel.startSensorScan(fieldUid)
    }

    // Auto-dismiss once data is received
    LaunchedEffect(status) {
        if (status.startsWith("Data received")) {
            Log.d("SensorBottomSheet", "Data received — dismissing dialog: $status")
            delay(1200) // brief pause so user sees the value
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
                viewModel.bleManager.stopScan()
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
                    // Data received — show value
                    status.startsWith("Data received") -> {
                        BluetoothSensorStatus(status = status, isSuccess = true)
                    }

                    // Error state
                    status == "No connections available" -> {
                        BluetoothSensorStatus(status = "No sensor found. Try again.", isSuccess = false)
                    }

                    // Connecting
                    status == "Connecting..." -> {
                        BluetoothSensorConnecting()
                    }

                    // Scanning (default — shown immediately on open)
                    else -> {
                        BluetoothSensorSearching()
                    }
                }
            }
        },
        buttonBlock = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                text = "CANCEL",
                onClick = {
                    coroutineScope.launch {
                        viewModel.bleManager.stopScan()
                        delay(100)
                        onDismiss()
                    }
                },
            )
        },
    )
}

/** Scanning — spinner shown immediately when dialog opens. */
@Composable
private fun BluetoothSensorSearching() {
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
            text = "Waiting for sensor...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing16))
        ProgressIndicator(type = ProgressIndicatorType.CIRCULAR_SMALL)
    }
}

/** Connecting state — GATT connection in progress. */
@Composable
private fun BluetoothSensorConnecting() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Connecting...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(Spacing.Spacing16))
        ProgressIndicator(type = ProgressIndicatorType.CIRCULAR_SMALL)
    }
}

/** Data received or error state. */
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

/** Device row — kept for optional manual-selection fallback. */
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
            Text(
                text = device.name ?: "Unknown Device",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
