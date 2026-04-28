package org.dhis2.form.ui.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.bluetooth.BluetoothDevice
import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dhis2.form.R
import org.dhis2.form.ui.FormViewModel
import org.dhis2.form.ui.intent.FormIntent
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
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        Log.d("SensorBottomSheet", "Opening connection dialog")
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            DHIS2Theme {
                SensorConnectionScreen(
                    fieldUid = fieldUid,
                    viewModel = viewModel,
                    onDismiss = {
                        Log.d("SensorBottomSheet", "Closing dialog safely")
                        dismissAllowingStateLoss()
                    }
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
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isScanningMap by viewModel.isFieldScanning.collectAsState()
    val isScanning = isScanningMap[fieldUid] ?: false
    val sensorStatuses by viewModel.sensorStatuses.collectAsState()
    val status = sensorStatuses[fieldUid] ?: ""
    val foundDevices by viewModel.foundDevices.collectAsState()

    LaunchedEffect(status) {
        if (status == "Connected") {
            delay(1000)
            onDismiss()
        }
    }

    BottomSheetShell(
        uiState = BottomSheetShellUIState(
            title = "Connect to Sensor",
            showTopSectionDivider = true
        ),
        onDismiss = {
            coroutineScope.launch {
                delay(100)
                onDismiss()
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Spacing16)
            ) {
                if (status.isEmpty() || status.startsWith("Disconnected") || status == "No connections available") {
                    // Bluetooth option
                    SensorOptionRow(
                        icon = R.drawable.ic_bluetooth,
                        label = "Bluetooth",
                        onClick = {
                            viewModel.submitIntent(FormIntent.OnSensorScanRequested(fieldUid, org.dhis2.sensor.connection.ConnectionType.BLE))
                        }
                    )
                    Spacer(modifier = Modifier.size(Spacing.Spacing8))
                    // USB option
                    SensorOptionRow(
                        icon = R.drawable.ic_usb,
                        label = "USB",
                        onClick = {
                            viewModel.submitIntent(FormIntent.OnSensorScanRequested(fieldUid, org.dhis2.sensor.connection.ConnectionType.USB))
                        }
                    )
                    Spacer(modifier = Modifier.size(Spacing.Spacing8))
                    // WiFi option
                    SensorOptionRow(
                        icon = R.drawable.ic_wifi,
                        label = "WiFi",
                        onClick = {
                            viewModel.submitIntent(FormIntent.OnSensorScanRequested(fieldUid, org.dhis2.sensor.connection.ConnectionType.WIFI))
                        }
                    )
                } else if (isScanning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProgressIndicator(type = ProgressIndicatorType.CIRCULAR_SMALL)
                        Spacer(modifier = Modifier.size(Spacing.Spacing16))
                        Text(
                            text = "Searching for available sensors...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        foundDevices.forEach { device ->
                           DeviceRow(device = device) {
                               viewModel.connectToDevice(device)
                           }
                        }
                    }
                } else {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (status == "Connected") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        buttonBlock = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                text = "CANCEL",
                onClick = {
                    coroutineScope.launch {
                        delay(100)
                        onDismiss()
                    }
                }
            )
        }
    )
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceRow(device: BluetoothDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.Spacing8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_bluetooth),
            contentDescription = null,
            modifier = Modifier.size(Spacing.Spacing24),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(Spacing.Spacing16))
        Column {
            Text(
                text = device.name ?: "Unknown Device",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SensorOptionRow(
    icon: Int,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.Spacing16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(Spacing.Spacing24),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.size(Spacing.Spacing16))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
