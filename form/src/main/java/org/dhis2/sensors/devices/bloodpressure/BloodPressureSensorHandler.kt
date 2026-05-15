package org.dhis2.sensors.devices.bloodpressure

import android.bluetooth.BluetoothGatt
import org.dhis2.sensors.core.BaseSensorHandler
import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.core.SensorType
import org.dhis2.sensors.utils.BluetoothUtils
import org.dhis2.sensors.utils.SensorLogger
import org.dhis2.sensors.utils.ValidationUtils

class BloodPressureSensorHandler : BaseSensorHandler {
    override val sensorType: SensorType = SensorType.BLOOD_PRESSURE

    override fun subscribe(gatt: BluetoothGatt) {
        val service = gatt.getService(BloodPressureConstants.serviceUuid)
        if (service == null) {
            SensorLogger.w(TAG, "Blood pressure service not found, enabling all notifiable characteristics")
            BluetoothUtils.enableAllNotifiableCharacteristics(gatt, TAG)
            return
        }

        val measurement = service.getCharacteristic(BloodPressureConstants.measurementUuid)
        if (measurement != null) {
            BluetoothUtils.enableCharacteristic(
                gatt = gatt,
                characteristic = measurement,
                cccdValue = byteArrayOf(0x02, 0x00),
            )
            SensorLogger.d(TAG, "Subscribed to blood pressure measurement")
        } else {
            SensorLogger.w(TAG, "Blood pressure measurement characteristic not found, enabling all")
            BluetoothUtils.enableAllNotifiableCharacteristics(gatt, TAG)
        }
    }

    override fun handleData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading> {
        val reading = BloodPressureParser.parse(data)
        if (!ValidationUtils.isValidBloodPressure(reading.systolic, reading.diastolic)) {
            SensorLogger.w(TAG, "Blood pressure values out of range, skipping packet")
            return emptyList()
        }

        val readings = mutableListOf(
            SensorReading(type = "SYSTOLIC", value = reading.systolic.toInt().toString(), unit = "mmHg"),
            SensorReading(type = "DIASTOLIC", value = reading.diastolic.toInt().toString(), unit = "mmHg"),
        )
        reading.pulseRate?.let { pulse ->
            if (ValidationUtils.isValidPulse(pulse)) {
                readings.add(
                    SensorReading(type = "PULSE", value = pulse.toInt().toString(), unit = "bpm"),
                )
            }
        }
        return readings
    }

    private companion object {
        const val TAG = "BLE_BP"
    }
}
