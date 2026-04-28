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
import java.util.UUID

private const val TAG_CONNECT = "BLE_CONNECT"
private const val TAG_SERVICE = "BLE_SERVICE"
private const val TAG_CHAR = "BLE_CHAR"

/** Standard Health Thermometer service and Temperature Measurement characteristic UUIDs. */
private val TEMP_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
private val TEMP_CHAR_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb")
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@SuppressLint("MissingPermission")
class BleDeviceConnector(
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onDataReceived: (String, String) -> Unit, // (uuid, parsedValue)
) {

    private var bluetoothGatt: BluetoothGatt? = null

    fun connect(context: Context, device: BluetoothDevice) {
        Log.d(TAG_CONNECT, "Connecting to target device: ${device.address}")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onConnectionStateChanged(false)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG_CONNECT, "Connected successfully to ${gatt.device.address}")
                    onConnectionStateChanged(true)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG_CONNECT, "Disconnected from ${gatt.device.address}")
                    onConnectionStateChanged(false)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG_SERVICE, "Services discovered (status=$status)")

            gatt.services.forEach { service ->
                Log.d(TAG_SERVICE, "Service UUID: ${service.uuid}")
                service.characteristics.forEach { characteristic ->
                    Log.d(TAG_CHAR, "  Characteristic UUID: ${characteristic.uuid}")
                }
            }

            // Target the temperature service specifically
            val tempService = gatt.getService(TEMP_SERVICE_UUID)
            if (tempService != null) {
                val tempChar = tempService.getCharacteristic(TEMP_CHAR_UUID)
                if (tempChar != null) {
                    Log.d(TAG_SERVICE, "Temperature characteristic found — enabling notifications")
                    gatt.setCharacteristicNotification(tempChar, true)

                    val descriptor = tempChar.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                } else {
                    Log.w(TAG_SERVICE, "Temperature characteristic not found — falling back to all notifiable characteristics")
                    enableAllNotifiableCharacteristics(gatt)
                }
            } else {
                Log.w(TAG_SERVICE, "Temperature service not found — falling back to all notifiable characteristics")
                enableAllNotifiableCharacteristics(gatt)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val value = characteristic.value ?: return
            val uuid = characteristic.uuid.toString().uppercase()

            val parsedValue = when {
                uuid.contains("2A1C") -> BleDataParser.parseTemperature(value).toString()
                uuid.contains("2A37") -> BleDataParser.parseHeartRate(value).toString()
                uuid.contains("2A19") -> BleDataParser.parseHeartRate(value).toString()
                uuid.contains("2A35") -> {
                    val bp = BleDataParser.parseBloodPressure(value)
                    "${bp.first}/${bp.second}"
                }
                uuid.contains("2A5E") || uuid.contains("2A5F") -> BleDataParser.parseSpO2(value).toString()
                else -> BleDataParser.parseHeartRate(value).toString()
            }

            Log.d("TEMP_VALUE", "Temperature = $parsedValue (UUID: $uuid)")
            onDataReceived(uuid, parsedValue)
        }
    }

    /** Fallback: enable notifications on every notifiable characteristic. */
    private fun enableAllNotifiableCharacteristics(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    gatt.setCharacteristicNotification(characteristic, true)

                    val descriptor = characteristic.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }
    }
}
