package org.dhis2.sensors.core

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import org.dhis2.sensors.devices.bloodpressure.BloodPressureParser
import org.dhis2.sensors.devices.temperature.TemperatureParser
import org.dhis2.sensors.utils.BluetoothUtils
import org.dhis2.sensors.utils.ByteUtils
import org.dhis2.sensors.utils.SensorLogger
import org.dhis2.sensors.utils.ValidationUtils

@SuppressLint("MissingPermission")
class BleConnector(
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onReadingsReceived: (List<SensorReading>) -> Unit,
    private val onConnectionFailed: (String) -> Unit = {},
) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var currentSensorType: SensorType = SensorType.UNKNOWN
    private var currentSensorHandler: BaseSensorHandler? = null

    fun connect(
        context: Context,
        device: BluetoothDevice,
        sensorType: SensorType,
    ) {
        closeCurrentGatt()
        currentSensorType = sensorType
        currentSensorHandler = SensorRegistry.getHandler(sensorType)
        SensorLogger.d(TAG_CONNECT, "Connecting to ${device.address} (type=$sensorType)")
        bluetoothGatt =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
    }

    fun disconnect() {
        currentSensorHandler?.onDisconnected()
        closeCurrentGatt()
        onConnectionStateChanged(false)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int,
        ) {
            if (!gatt.isCurrent()) {
                SensorLogger.w(TAG_CONNECT, "Ignoring stale GATT callback from ${gatt.device.address}")
                gatt.close()
                return
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                val message = "Connection failed for ${gatt.device.address} (status=$status)"
                SensorLogger.w(TAG_CONNECT, message)
                onConnectionFailed(message)
                currentSensorHandler?.onDisconnected()
                closeCurrentGatt()
                onConnectionStateChanged(false)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    SensorLogger.d(TAG_CONNECT, "Connected to ${gatt.device.address}")
                    onConnectionStateChanged(true)
                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    SensorLogger.d(TAG_CONNECT, "Disconnected from ${gatt.device.address} (status=$status)")
                    currentSensorHandler?.onDisconnected()
                    closeCurrentGatt()
                    onConnectionStateChanged(false)
                }
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int,
        ) {
            SensorLogger.d(TAG_SERVICE, "Services discovered (status=$status) for $currentSensorType")
            if (!gatt.isCurrent()) {
                SensorLogger.w(TAG_SERVICE, "Ignoring services from stale GATT ${gatt.device.address}")
                gatt.close()
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                val message = "Service discovery failed for ${gatt.device.address} (status=$status)"
                SensorLogger.w(TAG_SERVICE, message)
                onConnectionFailed(message)
                currentSensorHandler?.onDisconnected()
                closeCurrentGatt()
                onConnectionStateChanged(false)
                return
            }
            gatt.services.forEach { service ->
                SensorLogger.d(TAG_SERVICE, "Service: ${service.uuid}")
                service.characteristics.forEach { characteristic ->
                    SensorLogger.d(TAG_CHAR, "  Char: ${characteristic.uuid} props=${characteristic.properties}")
                }
            }
            currentSensorHandler = SensorRegistry.getHandler(currentSensorType)
            currentSensorHandler?.subscribe(gatt) ?: BluetoothUtils.enableAllNotifiableCharacteristics(gatt, TAG_SERVICE)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            val uuid = characteristic.uuid.toString().uppercase()
            SensorLogger.d("BLE_RAW", "Characteristic changed (API 33+) [$uuid] ${ByteUtils.formatBytes(value)}")
            dispatchReading(uuid, value)
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val data = characteristic.value ?: return
            val uuid = characteristic.uuid.toString().uppercase()
            SensorLogger.d("BLE_RAW", "Characteristic changed (legacy) [$uuid] ${ByteUtils.formatBytes(data)}")
            dispatchReading(uuid, data)
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value ?: return
                dispatchReading(characteristic.uuid.toString().uppercase(), data)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            SensorLogger.d(TAG_SERVICE, "Descriptor write for ${descriptor.characteristic.uuid} status=$status")
            currentSensorHandler?.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            currentSensorHandler?.onCharacteristicWrite(gatt, characteristic, status)
        }
    }

    private fun BluetoothGatt.isCurrent(): Boolean = bluetoothGatt === this

    private fun closeCurrentGatt() {
        val gatt = bluetoothGatt ?: return
        bluetoothGatt = null
        runCatching { gatt.disconnect() }
        runCatching { gatt.close() }
    }

    private fun dispatchReading(
        uuid: String,
        data: ByteArray,
    ) {
        val readings = currentSensorHandler?.handleData(uuid, data) ?: handleGenericData(uuid, data)
        if (readings.isNotEmpty()) {
            onReadingsReceived(readings)
        }
    }

    private fun handleGenericData(
        uuid: String,
        data: ByteArray,
    ): List<SensorReading> {
        return when {
            uuid.contains("2A1C") -> {
                val reading = TemperatureParser.parse(data)
                listOf(
                    SensorReading(
                        type = uuid,
                        value = String.format("%.1f", reading.value),
                        unit = reading.unit,
                    ),
                )
            }

            uuid.contains("2A35") -> {
                val reading = BloodPressureParser.parse(data)
                buildList {
                    add(SensorReading(type = "SYSTOLIC", value = reading.systolic.toInt().toString(), unit = "mmHg"))
                    add(SensorReading(type = "DIASTOLIC", value = reading.diastolic.toInt().toString(), unit = "mmHg"))
                    reading.pulseRate?.let { pulse ->
                        if (ValidationUtils.isValidPulse(pulse)) {
                            add(SensorReading(type = "PULSE", value = pulse.toInt().toString(), unit = "bpm"))
                        }
                    }
                }
            }

            uuid.contains("2A5E") || uuid.contains("2A5F") -> {
                val spo2Value = data.getOrNull(1)?.toInt()?.and(0xFF) ?: 0
                listOf(SensorReading(type = uuid, value = spo2Value.toString(), unit = "%"))
            }

            else -> {
                val heartRate = if (data.size < 2) 0 else data[1].toInt() and 0xFF
                listOf(SensorReading(type = uuid, value = heartRate.toString()))
            }
        }
    }

    private companion object {
        const val TAG_CONNECT = "BLE_CONNECT"
        const val TAG_SERVICE = "BLE_SERVICE"
        const val TAG_CHAR = "BLE_CHAR"
    }
}
