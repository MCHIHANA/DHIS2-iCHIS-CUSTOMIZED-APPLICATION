package org.dhis2.sensors.oximeter.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import org.dhis2.App
import org.dhis2.sensors.oximeter.viewmodel.OximeterViewModel
import javax.inject.Inject

/**
 * Activity for the FORA O2 Pulse Oximeter feature.
 */
class OximeterActivity : ComponentActivity() {

    @Inject
    lateinit var viewModel: OximeterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inject dependencies
        (application as App).appComponent.inject(this)

        setContent {
            MaterialTheme {
                Surface {
                    OximeterScreen(
                        viewModel = viewModel,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}
