package org.dhis2.sensors.devices.glucose

import android.bluetooth.BluetoothGatt
import org.dhis2.sensors.core.BaseSensorHandler
import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.core.SensorType

class GlucoseSensorHandler : BaseSensorHandler {
    override val sensorType: SensorType = SensorType.GLUCOSE

    override fun subscribe(gatt: BluetoothGatt) = Unit

    override fun handleData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading> = GlucoseParser.parse(data)
}
