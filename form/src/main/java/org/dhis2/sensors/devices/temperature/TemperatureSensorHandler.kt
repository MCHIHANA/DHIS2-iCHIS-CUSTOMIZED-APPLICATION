package org.dhis2.sensors.devices.temperature

import android.bluetooth.BluetoothGatt
import org.dhis2.sensors.core.BaseSensorHandler
import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.core.SensorType

class TemperatureSensorHandler : BaseSensorHandler {
    override val sensorType: SensorType = SensorType.TEMPERATURE

    override fun subscribe(gatt: BluetoothGatt) = Unit

    override fun handleData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading> = emptyList()
}
