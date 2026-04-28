package org.dhis2.form.ui.sensor.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class SensorRepository(context: Context) {

    private val bleScanner = BleScanner(context)
    private val bleConnectionManager = BleConnectionManager(context)

    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices = _foundDevices.asStateFlow()

    val connectionState: Flow<BleConnectionManager.ConnectionState> = bleConnectionManager.connectionState
    val sensorData: Flow<BleConnectionManager.SensorData?> = bleConnectionManager.sensorData

    val temperatureFlow: Flow<Float?> = sensorData.map {
        (it as? BleConnectionManager.SensorData.Temperature)?.value
    }

    val heartRateFlow: Flow<Int?> = sensorData.map {
        (it as? BleConnectionManager.SensorData.HeartRate)?.value
    }

    val bloodPressureFlow: Flow<Pair<Int, Int>?> = sensorData.map {
        (it as? BleConnectionManager.SensorData.BloodPressure)?.let { bp ->
            Pair(bp.systolic, bp.diastolic)
        }
    }

    val spo2Flow: Flow<Int?> = sensorData.map {
        (it as? BleConnectionManager.SensorData.SpO2)?.value
    }

    fun startScan() {
        _foundDevices.value = emptyList()
        bleScanner.startScan { device ->
            if (!_foundDevices.value.contains(device)) {
                _foundDevices.value = _foundDevices.value + device
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        bleConnectionManager.connect(device)
    }

    fun disconnect() {
        bleConnectionManager.disconnect()
    }
}
