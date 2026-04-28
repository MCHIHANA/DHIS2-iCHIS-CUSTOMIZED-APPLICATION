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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dhis2.form.R
import org.dhis2.sensor.connection.ConnectionType
import org.hisp.dhis.mobile.ui.designsystem.component.BottomSheetShell
import org.hisp.dhis.mobile.ui.designsystem.component.Button
import org.hisp.dhis.mobile.ui.designsystem.component.state.BottomSheetShellUIState
import org.hisp.dhis.mobile.ui.designsystem.theme.DHIS2Theme
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing

const val CONNECTION_SELECTION_TAG = "ConnectionSelectionBottomSheet"

/**
 * Bottom sheet for selecting sensor connection type
 * Shows options: Bluetooth, USB, WiFi
 */
class ConnectionSelectionBottomSheet(
    private val fieldUid: String,
    private val onConnectionTypeSelected: (ConnectionType) -> Unit,
    private val onDismiss: () -> Unit = {}
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
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            DHIS2Theme {
                ConnectionSelectionScreen(
                    onConnectionTypeSelected = { connectionType ->
                        onConnectionTypeSelected(connectionType)
                        dismissAllowingStateLoss()
                    },
                    onDismiss = {
                        onDismiss()
                        dismissAllowingStateLoss()
                    }
                )
            }
        }
    }

    fun show(manager: FragmentManager) {
        super.show(manager, CONNECTION_SELECTION_TAG)
    }
}

@Composable
fun ConnectionSelectionScreen(
    onConnectionTypeSelected: (ConnectionType) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    BottomSheetShell(
        uiState = BottomSheetShellUIState(
            title = "Select Connection Type",
            subtitle = "Choose how to connect to your sensor",
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
                // Bluetooth option
                ConnectionOptionRow(
                    icon = R.drawable.ic_bluetooth,
                    label = "Bluetooth",
                    description = "Connect via Bluetooth Low Energy",
                    onClick = { onConnectionTypeSelected(ConnectionType.BLE) }
                )

                Spacer(modifier = Modifier.size(Spacing.Spacing8))

                // USB option
                ConnectionOptionRow(
                    icon = R.drawable.ic_usb,
                    label = "USB",
                    description = "Connect via USB cable",
                    onClick = { onConnectionTypeSelected(ConnectionType.USB) }
                )

                Spacer(modifier = Modifier.size(Spacing.Spacing8))

                // WiFi option
                ConnectionOptionRow(
                    icon = R.drawable.ic_wifi,
                    label = "WiFi",
                    description = "Connect via wireless network",
                    onClick = { onConnectionTypeSelected(ConnectionType.WIFI) }
                )
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

@Composable
private fun ConnectionOptionRow(
    icon: Int,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.Spacing12, horizontal = Spacing.Spacing8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier
                .padding(start = Spacing.Spacing16)
                .weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
