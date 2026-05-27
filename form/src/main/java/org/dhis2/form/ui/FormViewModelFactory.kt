package org.dhis2.form.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.dhis2.commons.viewmodel.DispatcherProvider
import org.dhis2.form.data.FormRepository
import org.dhis2.form.ui.provider.FormResultDialogProvider
import org.dhis2.sensor.ble.BleManager
import org.dhis2.sensor.config.SensorConfigRepository
import org.dhis2.sensors.device_manager.PairedDeviceRepository
import org.dhis2.sensors.device_manager.ReconnectManager

class FormViewModelFactory(
    private val repository: FormRepository,
    private val dispatcher: DispatcherProvider,
    private val bleManager: BleManager,
    private val sensorConfigRepository: SensorConfigRepository,
    private val pairedDeviceRepository: PairedDeviceRepository,
    private val reconnectManager: ReconnectManager,
    private val openErrorLocation: Boolean,
    private val resultDialogUiProvider: FormResultDialogProvider,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FormViewModel(
            repository = repository,
            dispatcher = dispatcher,
            bleManager = bleManager,
            sensorConfigRepository = sensorConfigRepository,
            pairedDeviceRepository = pairedDeviceRepository,
            reconnectManager = reconnectManager,
            openErrorLocation = openErrorLocation,
            resultDialogUiProvider = resultDialogUiProvider,
        ) as T
}
