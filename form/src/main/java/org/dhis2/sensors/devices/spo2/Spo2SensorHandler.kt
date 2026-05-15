package org.dhis2.sensors.devices.spo2

import android.bluetooth.BluetoothGatt
import org.dhis2.sensors.core.BaseSensorHandler
import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.core.SensorType

class Spo2SensorHandler : BaseSensorHandler {
    override val sensorType: SensorType = SensorType.SPO2

    override fun subscribe(gatt: BluetoothGatt) = Unit

    override fun handleData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading> = emptyList()
}
