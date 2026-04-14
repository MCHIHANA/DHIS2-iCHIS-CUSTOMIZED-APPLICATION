package org.dhis2.form.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            DHIS2Theme {
                SensorConnectionScreen(
                    fieldUid = fieldUid,
                    viewModel = viewModel,
                    onDismiss = { dismiss() }
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
    val isScanningMap by viewModel.isFieldScanning.collectAsState()
    val isScanning = isScanningMap[fieldUid] ?: false
    val sensorStatuses by viewModel.sensorStatuses.collectAsState()
    val status = sensorStatuses[fieldUid] ?: ""

    BottomSheetShell(
        uiState = BottomSheetShellUIState(
            title = "Connect to Sensor",
            showTopSectionDivider = true
        ),
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Spacing16)
            ) {
                if (isScanning) {
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
                    }
                } else if (status == "Sensor connected") {
                    // Mission accomplished, we can dismiss it after a brief shown success
                    // Or let the field show it.
                    onDismiss()
                } else {
                    SensorOptionRow(
                        icon = R.drawable.ic_bluetooth,
                        label = "Bluetooth",
                        onClick = { viewModel.submitIntent(FormIntent.OnSensorScanRequested(fieldUid, "Bluetooth")) }
                    )
                    SensorOptionRow(
                        icon = R.drawable.ic_usb,
                        label = "USB",
                        onClick = { viewModel.submitIntent(FormIntent.OnSensorScanRequested(fieldUid, "USB")) }
                    )
                    SensorOptionRow(
                        icon = R.drawable.ic_wifi,
                        label = "WiFi",
                        onClick = { viewModel.submitIntent(FormIntent.OnSensorScanRequested(fieldUid, "WiFi")) }
                    )
                    
                    if (status.isNotEmpty() && status.startsWith("Sensor not found")) {
                        Spacer(modifier = Modifier.size(Spacing.Spacing8))
                        Text(
                            text = status,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        buttonBlock = {
            if (!isScanning) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    text = "CANCEL",
                    onClick = onDismiss
                )
            }
        }
    )
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
