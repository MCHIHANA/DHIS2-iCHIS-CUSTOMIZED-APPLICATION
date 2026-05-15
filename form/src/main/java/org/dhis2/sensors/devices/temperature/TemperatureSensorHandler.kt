package org.dhis2.sensors.devices.temperature

import android.bluetooth.BluetoothGatt
import org.dhis2.sensors.core.BaseSensorHandler
import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.core.SensorType
import org.dhis2.sensors.utils.BluetoothUtils
import org.dhis2.sensors.utils.SensorLogger

class TemperatureSensorHandler : BaseSensorHandler {
    override val sensorType: SensorType = SensorType.TEMPERATURE

    override fun subscribe(gatt: BluetoothGatt) {
        val service = gatt.getService(TemperatureConstants.serviceUuid)
        if (service == null) {
            SensorLogger.w(TAG, "Health thermometer service not found, enabling all notifiable characteristics")
            BluetoothUtils.enableAllNotifiableCharacteristics(gatt, TAG)
            return
        }

        val measurement = service.getCharacteristic(TemperatureConstants.measurementUuid)
        if (measurement != null) {
            BluetoothUtils.enableCharacteristic(
                gatt = gatt,
                characteristic = measurement,
                cccdValue = byteArrayOf(0x02, 0x00),
            )
            SensorLogger.d(TAG, "Subscribed to temperature measurement")
        } else {
            SensorLogger.w(TAG, "Temperature measurement characteristic not found, enabling all")
            BluetoothUtils.enableAllNotifiableCharacteristics(gatt, TAG)
        }
    }

    override fun handleData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading> {
        val reading = TemperatureParser.parse(data)
        return listOf(
            SensorReading(
                type = uuid,
                value = String.format("%.1f", reading.value),
                unit = reading.unit,
            ),
        )
    }

    private companion object {
        const val TAG = "BLE_TEMP"
    }
}
