package org.dhis2.sensors.devices.bloodpressure

import android.bluetooth.BluetoothGatt
import org.dhis2.sensors.core.BaseSensorHandler
import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.core.SensorType

class BloodPressureSensorHandler : BaseSensorHandler {
    override val sensorType: SensorType = SensorType.BLOOD_PRESSURE

    override fun subscribe(gatt: BluetoothGatt) = Unit

    override fun handleData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading> = emptyList()
}
