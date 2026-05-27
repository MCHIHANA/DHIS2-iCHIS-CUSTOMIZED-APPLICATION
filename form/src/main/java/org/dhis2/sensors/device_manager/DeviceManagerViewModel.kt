package org.dhis2.sensors.device_manager

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.dhis2.sensor.config.SensorConfigApi
import org.dhis2.sensor.config.SensorConfigRepository
import org.hisp.dhis.android.core.D2Manager

class DeviceManagerViewModel(
    private val pairedDeviceRepository: PairedDeviceRepository,
    private val deviceConnectionService: DeviceConnectionService,
) : ViewModel() {
    val pairedDevices: StateFlow<List<SensorDevice>> = pairedDeviceRepository.devices
    val availableDevices: StateFlow<List<BluetoothDevice>> = deviceConnectionService.availableDevices
    val pairingDeviceType: StateFlow<DeviceType?> = deviceConnectionService.pairingDeviceType
    val connectionState = deviceConnectionService.connectionState
    val currentDeviceAddress = deviceConnectionService.currentDeviceAddress

    private val _isReceivingData = MutableStateFlow(false)
    val isReceivingData: StateFlow<Boolean> = _isReceivingData.asStateFlow()

    init {
        deviceConnectionService.sensorData
            .onEach { readings -> _isReceivingData.value = readings.isNotEmpty() }
            .launchIn(viewModelScope)
    }

    fun refreshDevices() {
        pairedDeviceRepository.refresh()
    }

    fun startPairing(deviceType: DeviceType) {
        deviceConnectionService.startPairingScan(deviceType)
    }

    fun stopPairing() {
        deviceConnectionService.stopPairingScan()
    }

    fun savePairedDevice(device: BluetoothDevice) {
        deviceConnectionService.savePairedDevice(device)
    }

    fun connect(device: SensorDevice) {
        deviceConnectionService.connectToDevice(device)
    }

    fun disconnect() {
        deviceConnectionService.disconnect()
    }

    fun removeDevice(macAddress: String) {
        if (currentDeviceAddress.value.equals(macAddress, ignoreCase = true)) {
            deviceConnectionService.disconnect()
        }
        pairedDeviceRepository.removeDevice(macAddress)
    }

    override fun onCleared() {
        deviceConnectionService.clear()
        super.onCleared()
    }
}

class DeviceManagerViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val applicationContext = appContext.applicationContext
        val sensorConfigRepository =
            SensorConfigRepository(
                applicationContext,
                SensorConfigApi(D2Manager.getD2()),
            )
        val pairedDeviceRepository =
            PairedDeviceRepository(
                DeviceStorageManager(applicationContext),
            )
        val deviceConnectionService =
            DeviceConnectionService(
                context = applicationContext,
                pairedDeviceRepository = pairedDeviceRepository,
                sensorConfigRepository = sensorConfigRepository,
            )

        @Suppress("UNCHECKED_CAST")
        return DeviceManagerViewModel(
            pairedDeviceRepository = pairedDeviceRepository,
            deviceConnectionService = deviceConnectionService,
        ) as T
    }
}
