package org.dhis2.sensors.core

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

interface BaseSensorHandler {
    val sensorType: SensorType

    fun subscribe(gatt: BluetoothGatt)

    fun handleData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading>

    fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
    ) = Unit

    fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) = Unit

    fun onDisconnected() = Unit
}
