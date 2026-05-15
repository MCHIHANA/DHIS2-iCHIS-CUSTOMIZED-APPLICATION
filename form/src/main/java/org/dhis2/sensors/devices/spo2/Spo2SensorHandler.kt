package org.dhis2.sensors.devices.spo2

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Handler
import android.os.Looper
import org.dhis2.sensors.core.BaseSensorHandler
import org.dhis2.sensors.core.SensorReading
import org.dhis2.sensors.core.SensorType
import org.dhis2.sensors.utils.BluetoothUtils
import org.dhis2.sensors.utils.ByteUtils
import org.dhis2.sensors.utils.SensorLogger

class Spo2SensorHandler : BaseSensorHandler {
    override val sensorType: SensorType = SensorType.SPO2
    private val retryHandler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null

    @Volatile
    private var dataReceived = false

    override fun subscribe(gatt: BluetoothGatt) {
        SensorLogger.d(TAG, "Subscribing to FORA O2 service")
        val service = gatt.getService(Spo2Constants.serviceUuid)
        if (service == null) {
            SensorLogger.w(TAG, "FORA O2 service not found, enabling all notifiable characteristics")
            BluetoothUtils.enableAllNotifiableCharacteristics(gatt, TAG)
            return
        }

        val measurement = service.getCharacteristic(Spo2Constants.measurementUuid)
        if (measurement == null) {
            SensorLogger.w(TAG, "FORA O2 characteristic not found, enabling all notifiable characteristics")
            BluetoothUtils.enableAllNotifiableCharacteristics(gatt, TAG)
            return
        }

        gatt.setCharacteristicNotification(measurement, true)
        val descriptor = measurement.getDescriptor(Spo2Constants.cccdUuid)
        if (descriptor != null) {
            BluetoothUtils.writeDescriptor(
                gatt = gatt,
                descriptor = descriptor,
                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
            )
            SensorLogger.d(TAG, "Notification descriptor write requested for FORA O2")
        } else {
            SensorLogger.d(TAG, "FORA O2 CCCD missing, sending trigger directly")
            sendTriggerCommand(gatt, measurement)
        }
    }

    override fun handleData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading> {
        SensorLogger.d(TAG, "FORA O2 raw (${data.size} bytes): ${ByteUtils.formatBytes(data)}")
        if (!dataReceived) {
            dataReceived = true
            retryRunnable?.let(retryHandler::removeCallbacks)
            SensorLogger.d(TAG, "First SPO2 packet received, cancelling retry timer")
        }
        return Spo2Parser.parse(data)
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            SensorLogger.w(TAG, "Descriptor write failed for ${descriptor.characteristic.uuid} with status=$status")
            return
        }
        if (descriptor.characteristic.uuid == Spo2Constants.measurementUuid) {
            sendTriggerCommand(gatt, descriptor.characteristic)
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        SensorLogger.d(TAG, "Characteristic write completed for ${characteristic.uuid} with status=$status")
        if (status != BluetoothGatt.GATT_SUCCESS) {
            SensorLogger.w(TAG, "Trigger write failed, retrying with WRITE_TYPE_DEFAULT")
            BluetoothUtils.writeCharacteristic(
                gatt = gatt,
                characteristic = characteristic,
                value = Spo2Constants.triggerCommand,
                writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            )
        }
    }

    override fun onDisconnected() {
        retryRunnable?.let(retryHandler::removeCallbacks)
        retryRunnable = null
        dataReceived = false
    }

    private fun sendTriggerCommand(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        dataReceived = false
        BluetoothUtils.writeCharacteristic(
            gatt = gatt,
            characteristic = characteristic,
            value = Spo2Constants.triggerCommand,
            writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
        )
        scheduleRetryTrigger(gatt, characteristic)
    }

    private fun scheduleRetryTrigger(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        retryRunnable?.let(retryHandler::removeCallbacks)
        retryRunnable = Runnable {
            if (!dataReceived) {
                SensorLogger.w(TAG, "No SPO2 data after 2 seconds, retrying trigger")
                BluetoothUtils.writeCharacteristic(
                    gatt = gatt,
                    characteristic = characteristic,
                    value = Spo2Constants.triggerCommand,
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                )
            }
        }.also { retryHandler.postDelayed(it, 2000L) }
    }

    private companion object {
        const val TAG = "BLE_SPO2"
    }
}
