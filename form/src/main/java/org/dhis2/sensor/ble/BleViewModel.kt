package org.dhis2.sensor.ble

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BleViewModel(
    private val bleManager: BleManager
) : ViewModel() {

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _sensorValue = MutableStateFlow<String>("")
    val sensorValue = _sensorValue.asStateFlow()

    init {
        bleManager.devices.onEach {
            _devices.value = it
        }.launchIn(viewModelScope)

        bleManager.sensorData.onEach { data ->
            if (data != null) {
                _sensorValue.value = data.second
            }
        }.launchIn(viewModelScope)
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectDevice(device: BluetoothDevice) {
        bleManager.connectDevice(device)
    }
}
