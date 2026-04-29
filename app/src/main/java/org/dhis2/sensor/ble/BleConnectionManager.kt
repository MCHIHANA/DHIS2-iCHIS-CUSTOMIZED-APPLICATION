package org.dhis2.sensor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleConnectionManager(private val context: Context) {

    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData = _sensorData.asStateFlow()

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING
    }

    sealed class SensorData {
        data class Temperature(val value: Float) : SensorData()
        data class HeartRate(val value: Int) : SensorData()
        data class BloodPressure(val systolic: Int, val diastolic: Int) : SensorData()
        data class SpO2(val value: Int) : SensorData()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.d("BLE_CONN", "Connected to GATT server.")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Log.d("BLE_CONN", "Disconnected from GATT server.")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE_CONN", "Services discovered")
                enableNotifications(gatt)
            } else {
                Log.w("BLE_CONN", "onServicesDiscovered received: $status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            Log.d("BLE_RAW", data.joinToString())
            
            val parsedData = when (characteristic.uuid) {
                BleHealthUUIDs.TEMPERATURE_MEASUREMENT_CHAR -> {
                    SensorData.Temperature(BleDataParser.parseTemperature(data))
                }
                BleHealthUUIDs.HEART_RATE_MEASUREMENT_CHAR -> {
                    SensorData.HeartRate(BleDataParser.parseHeartRate(data))
                }
                BleHealthUUIDs.BLOOD_PRESSURE_MEASUREMENT_CHAR -> {
                    val bp = BleDataParser.parseBloodPressure(data)
                    SensorData.BloodPressure(bp.first, bp.second)
                }
                BleHealthUUIDs.SPO2_MEASUREMENT_CHAR -> {
                    SensorData.SpO2(BleDataParser.parseSpO2(data))
                }
                else -> null
            }

            parsedData?.let {
                _sensorData.value = it
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTING
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt) {
        val services = gatt.services
        for (service in services) {
            for (characteristic in service.characteristics) {
                if (isHealthCharacteristic(characteristic.uuid)) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(BleHealthUUIDs.CLIENT_CHARACTERISTIC_CONFIG)
                    descriptor?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(it)
                    }
                }
            }
        }
    }

    private fun isHealthCharacteristic(uuid: java.util.UUID): Boolean {
        return uuid == BleHealthUUIDs.TEMPERATURE_MEASUREMENT_CHAR ||
               uuid == BleHealthUUIDs.HEART_RATE_MEASUREMENT_CHAR ||
               uuid == BleHealthUUIDs.BLOOD_PRESSURE_MEASUREMENT_CHAR ||
               uuid == BleHealthUUIDs.SPO2_MEASUREMENT_CHAR
    }
}
