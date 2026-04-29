package org.dhis2.sensor.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SensorRepository(context: Context) {

    private val bleConnectionManager = BleConnectionManager(context)

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

    fun connect(device: BluetoothDevice) {
        bleConnectionManager.connect(device)
    }

    fun disconnect() {
        bleConnectionManager.disconnect()
    }
}
