package org.dhis2.form.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
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
 * Bottom sheet for initiating a Bluetooth sensor connection.
 * USB and WiFi options have been removed — Bluetooth is the only supported transport.
 */
class ConnectionSelectionBottomSheet(
    private val fieldUid: String,
    private val onConnectionTypeSelected: (ConnectionType) -> Unit,
    private val onDismiss: () -> Unit = {},
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
                    },
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
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    BottomSheetShell(
        uiState = BottomSheetShellUIState(
            title = "Connect Sensor",
            subtitle = "Bluetooth Low Energy (BLE)",
            showTopSectionDivider = true,
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
                    .padding(Spacing.Spacing16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bluetooth),
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.Spacing48),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(Spacing.Spacing8))
                Text(
                    text = "The app will automatically detect and connect to your medical sensor via Bluetooth.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.Spacing16))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    text = "START BLUETOOTH SCAN",
                    onClick = { onConnectionTypeSelected(ConnectionType.BLE) },
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
                },
            )
        },
    )
}
