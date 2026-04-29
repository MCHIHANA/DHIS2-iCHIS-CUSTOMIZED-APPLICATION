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
private const val TAG_CHAR    = "BLE_CHAR"

/** Standard Health Thermometer service (0x1809). */
private val TEMP_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")

/**
 * Temperature Measurement characteristic (0x2A1C).
 * This characteristic uses INDICATE (not NOTIFY) per the BLE spec.
 */
private val TEMP_CHAR_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb")

/** Client Characteristic Configuration Descriptor — enables notifications/indications. */
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@SuppressLint("MissingPermission")
class BleDeviceConnector(
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onDataReceived: (String, String) -> Unit, // (uuid, parsedValue)
) {

    private var bluetoothGatt: BluetoothGatt? = null

    fun connect(context: Context, device: BluetoothDevice) {
        Log.d(TAG_CONNECT, "Connecting to ${device.address}...")
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
                    Log.d(TAG_CONNECT, "Connected to ${gatt.device.address}")
                    onConnectionStateChanged(true)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG_CONNECT, "Disconnected from ${gatt.device.address}")
                    onConnectionStateChanged(false)
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG_SERVICE, "Services discovered (status=$status)")

            gatt.services.forEach { service ->
                Log.d(TAG_SERVICE, "Service: ${service.uuid}")
                service.characteristics.forEach { c ->
                    Log.d(TAG_CHAR, "  Characteristic: ${c.uuid} | props=${c.properties}")
                }
            }

            val service = gatt.getService(TEMP_SERVICE_UUID)
            if (service == null) {
                Log.w(TAG_SERVICE, "Health Thermometer service not found — enabling all notifiable characteristics as fallback")
                enableAllNotifiableCharacteristics(gatt)
                return
            }

            val characteristic = service.getCharacteristic(TEMP_CHAR_UUID)
            if (characteristic == null) {
                Log.w(TAG_SERVICE, "Temperature Measurement characteristic not found")
                return
            }

            // 0x2A1C uses INDICATE (bit 5), not NOTIFY (bit 4).
            // We enable both in the CCCD to handle devices that use either.
            gatt.setCharacteristicNotification(characteristic, true)
            Log.d(TAG_SERVICE, "Subscribed to temperature characteristic")

            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                // ENABLE_INDICATION_VALUE = 0x02, ENABLE_NOTIFICATION_VALUE = 0x01
                // Write both bits to handle devices that use either mode
                descriptor.value = byteArrayOf(0x02, 0x00) // INDICATE
                gatt.writeDescriptor(descriptor)
                Log.d(TAG_SERVICE, "CCCD written — indications enabled")
            } else {
                Log.w(TAG_SERVICE, "CCCD descriptor not found on temperature characteristic")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val data = characteristic.value ?: return
            val uuid = characteristic.uuid.toString().uppercase()

            Log.d("BLE_RAW", "Characteristic $uuid data: ${data.joinToString { "%02X".format(it) }}")

            val parsedValue: String = when {
                uuid.contains("2A1C") -> {
                    // Try raw format first (tempRaw / 100.0), then IEEE-11073 fallback
                    val raw = BleDataParser.parseTemperatureRaw(data)
                    val value = if (raw in 30.0..45.0) raw else BleDataParser.parseTemperature(data).toDouble()
                    Log.d("BLE_TEMP", "Temperature: $value °C")
                    "%.1f".format(value)
                }
                uuid.contains("2A37") -> BleDataParser.parseHeartRate(data).toString()
                uuid.contains("2A35") -> {
                    val bp = BleDataParser.parseBloodPressure(data)
                    "${bp.first}/${bp.second}"
                }
                uuid.contains("2A5E") || uuid.contains("2A5F") ->
                    BleDataParser.parseSpO2(data).toString()
                else -> BleDataParser.parseHeartRate(data).toString()
            }

            onDataReceived(uuid, parsedValue)
        }

        // Also handle indications (same callback, different callbackType)
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                onCharacteristicChanged(gatt, characteristic)
            }
        }
    }

    /** Fallback: subscribe to every notifiable/indicatable characteristic. */
    private fun enableAllNotifiableCharacteristics(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                val props = characteristic.properties
                val canNotify = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                val canIndicate = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0

                if (canNotify || canIndicate) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        descriptor.value = when {
                            canIndicate -> byteArrayOf(0x02, 0x00)
                            else -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        }
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }
    }
}
