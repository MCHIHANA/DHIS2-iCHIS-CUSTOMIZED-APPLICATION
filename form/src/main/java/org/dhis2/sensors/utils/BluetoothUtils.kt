package org.dhis2.sensors.utils

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build

object BluetoothUtils {
    fun writeDescriptor(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = value
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    fun writeCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        writeType: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, value, writeType)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    fun enableCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        cccdValue: ByteArray,
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(standardCccdUuid)
        if (descriptor != null) {
            writeDescriptor(gatt, descriptor, cccdValue)
        }
    }

    fun enableAllNotifiableCharacteristics(
        gatt: BluetoothGatt,
        tag: String,
    ) {
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                val properties = characteristic.properties
                val canNotify = (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                val canIndicate = (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                if (canNotify || canIndicate) {
                    val cccdValue = if (canIndicate && !canNotify) {
                        byteArrayOf(0x02, 0x00)
                    } else {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    }
                    SensorLogger.d(tag, "Enabling notifications for ${characteristic.uuid}")
                    enableCharacteristic(gatt, characteristic, cccdValue)
                }
            }
        }
    }

    val standardCccdUuid = java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
