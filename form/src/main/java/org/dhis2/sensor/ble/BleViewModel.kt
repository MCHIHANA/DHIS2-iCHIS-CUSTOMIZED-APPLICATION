package org.dhis2.sensor.ble

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BleViewModel(
    private val bleManager: BleManager,
) : ViewModel() {

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    /** First reading value (SpO2 or temperature). */
    private val _sensorValue = MutableStateFlow("")
    val sensorValue = _sensorValue.asStateFlow()

    /** Second reading value (pulse rate for oximeter). Empty for single-value sensors. */
    private val _secondaryValue = MutableStateFlow("")
    val secondaryValue = _secondaryValue.asStateFlow()

    init {
        bleManager.devices.onEach {
            _devices.value = it
        }.launchIn(viewModelScope)

        // sensorData is now List<Pair<String,String>> — take first and second entries
        bleManager.sensorData.onEach { readings ->
            _sensorValue.value   = readings.getOrNull(0)?.second ?: ""
            _secondaryValue.value = readings.getOrNull(1)?.second ?: ""
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
