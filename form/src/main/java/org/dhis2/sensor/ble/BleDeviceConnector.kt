package org.dhis2.sensor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.util.UUID

private const val TAG_CONNECT = "BLE_CONNECT"
private const val TAG_SERVICE = "BLE_SERVICE"
private const val TAG_CHAR    = "BLE_CHAR"

// ── Standard BLE SIG UUIDs ────────────────────────────────────────────────────

/** Health Thermometer service (0x1809) */
private val TEMP_SERVICE_UUID  = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
/** Temperature Measurement characteristic (0x2A1C) — uses INDICATE */
private val TEMP_CHAR_UUID     = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb")
/** PLX Spot-check Measurement (0x2A5E) — uses INDICATE */
private val PLX_SPOT_UUID      = UUID.fromString("00002A5E-0000-1000-8000-00805f9b34fb")
/** PLX Continuous Measurement (0x2A5F) — uses NOTIFY */
private val PLX_CONT_UUID      = UUID.fromString("00002A5F-0000-1000-8000-00805f9b34fb")
/** Client Characteristic Configuration Descriptor */
private val CCCD_UUID          = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// ── FORA O2 custom Nordic service UUIDs ──────────────────────────────────────
// The FORA O2 uses a Nordic LED Button Service variant to send SpO2 + pulse data.

/** Nordic LED Button Service */
private val NORDIC_SERVICE_UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
/** Nordic Button characteristic — device sends SpO2/pulse readings here via NOTIFY */
private val NORDIC_BUTTON_UUID  = UUID.fromString("00001524-1212-efde-1523-785feabcd123")
/** Nordic LED characteristic — used to send commands to the device */
private val NORDIC_LED_UUID     = UUID.fromString("00001525-1212-efde-1523-785feabcd123")

@SuppressLint("MissingPermission")
class BleDeviceConnector(
    private val onConnectionStateChanged: (Boolean) -> Unit,
    /** Called with a list of (key, value) pairs. Keys: "SPO2", "PULSE", UUID strings. */
    private val onReadingsReceived: (List<Pair<String, String>>) -> Unit,
) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentSensorType: SensorType = SensorType.UNKNOWN

    fun connect(context: Context, device: BluetoothDevice, sensorType: SensorType) {
        currentSensorType = sensorType
        Log.d(TAG_CONNECT, "Connecting to ${device.address} (type=$sensorType)...")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onConnectionStateChanged(false)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG_CONNECT, "Connected to ${gatt.device.address}")
                    onConnectionStateChanged(true)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG_CONNECT, "Disconnected from ${gatt.device.address}")
                    onConnectionStateChanged(false)
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG_SERVICE, "Services discovered (status=$status) for $currentSensorType")
            gatt.services.forEach { service ->
                Log.d(TAG_SERVICE, "Service: ${service.uuid}")
                service.characteristics.forEach { c ->
                    Log.d(TAG_CHAR, "  Char: ${c.uuid} props=${c.properties}")
                }
            }

            when (currentSensorType) {
                SensorType.SPO2 -> subscribeForaO2(gatt)
                SensorType.TEMPERATURE -> subscribeThermometer(gatt)
                else -> enableAllNotifiableCharacteristics(gatt)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val data = characteristic.value ?: return
            val uuid = characteristic.uuid.toString().uppercase()
            Log.d("BLE_RAW", "[$uuid] ${data.joinToString { "%02X".format(it) }}")

            when (currentSensorType) {
                SensorType.SPO2 -> handleForaO2Data(uuid, data)
                SensorType.TEMPERATURE -> handleTemperatureData(uuid, data)
                else -> handleGenericData(uuid, data)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                onCharacteristicChanged(gatt, characteristic)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            Log.d(TAG_SERVICE, "Descriptor written: ${descriptor.characteristic.uuid} status=$status")
        }
    }

    // ── FORA O2 (SpO2 + Pulse) ───────────────────────────────────────────────

    /**
     * Subscribes to the FORA O2's Nordic custom characteristic.
     * The device sends SpO2 and pulse rate as NOTIFY on the button characteristic.
     *
     * Fallback: if the Nordic service isn't found, enable all notifiable
     * characteristics so we catch whatever the device sends.
     */
    private fun subscribeForaO2(gatt: BluetoothGatt) {
        val service = gatt.getService(NORDIC_SERVICE_UUID)
        if (service == null) {
            Log.w(TAG_SERVICE, "Nordic service not found on FORA O2 — trying fallback")
            // Try standard PLX characteristics as secondary fallback
            val plxSpot = gatt.getService(UUID.fromString("00001822-0000-1000-8000-00805f9b34fb"))
                ?.getCharacteristic(PLX_SPOT_UUID)
            if (plxSpot != null) {
                enableCharacteristic(gatt, plxSpot, indicate = true)
                Log.d(TAG_SERVICE, "Subscribed to PLX Spot-check characteristic")
            } else {
                enableAllNotifiableCharacteristics(gatt)
            }
            return
        }

        val buttonChar = service.getCharacteristic(NORDIC_BUTTON_UUID)
        if (buttonChar != null) {
            enableCharacteristic(gatt, buttonChar, indicate = false) // NOTIFY
            Log.d(TAG_SERVICE, "Subscribed to FORA O2 Nordic button characteristic")
        } else {
            Log.w(TAG_SERVICE, "Nordic button characteristic not found — enabling all")
            enableAllNotifiableCharacteristics(gatt)
        }
    }

    /**
     * Parses FORA O2 data packet from the Nordic button characteristic.
     *
     * FORA O2 packet format (observed, 4–6 bytes):
     *   Byte 0: packet type / status flags
     *   Byte 1: SpO2 value (0–100 %)
     *   Byte 2: Pulse rate low byte
     *   Byte 3: Pulse rate high byte (if > 255 bpm, rare)
     *
     * Values of 0x7F (127) or 0xFF (255) indicate "no reading yet" — ignored.
     */
    private fun handleForaO2Data(uuid: String, data: ByteArray) {
        if (data.size < 3) {
            Log.d(TAG_SERVICE, "FORA O2 packet too short (${data.size} bytes) — waiting")
            return
        }

        val spo2 = data[1].toInt() and 0xFF
        val pulse = (data[2].toInt() and 0xFF) or
            (if (data.size >= 4) (data[3].toInt() and 0xFF) shl 8 else 0)

        // 0x7F / 0xFF = sensor still initialising, skip
        if (spo2 == 0x7F || spo2 == 0xFF || spo2 == 0) {
            Log.d(TAG_SERVICE, "FORA O2 — sensor initialising (spo2=$spo2), waiting...")
            return
        }

        Log.d("BLE_SPO2", "SpO2=$spo2% Pulse=$pulse bpm")

        onReadingsReceived(
            listOf(
                Pair("SPO2",  spo2.toString()),
                Pair("PULSE", pulse.toString()),
            ),
        )
    }

    // ── Thermometer ──────────────────────────────────────────────────────────

    private fun subscribeThermometer(gatt: BluetoothGatt) {
        val service = gatt.getService(TEMP_SERVICE_UUID)
        if (service == null) {
            Log.w(TAG_SERVICE, "Health Thermometer service not found — enabling all")
            enableAllNotifiableCharacteristics(gatt)
            return
        }
        val char = service.getCharacteristic(TEMP_CHAR_UUID)
        if (char != null) {
            enableCharacteristic(gatt, char, indicate = true) // 0x2A1C uses INDICATE
            Log.d(TAG_SERVICE, "Subscribed to Temperature Measurement characteristic")
        } else {
            Log.w(TAG_SERVICE, "Temperature characteristic not found — enabling all")
            enableAllNotifiableCharacteristics(gatt)
        }
    }

    private fun handleTemperatureData(uuid: String, data: ByteArray) {
        val raw = BleDataParser.parseTemperatureRaw(data)
        val value = if (raw in 30.0..45.0) raw else BleDataParser.parseTemperature(data).toDouble()
        Log.d("BLE_TEMP", "Temperature: $value °C")
        onReadingsReceived(listOf(Pair(uuid, "%.1f".format(value))))
    }

    // ── Generic fallback ─────────────────────────────────────────────────────

    private fun handleGenericData(uuid: String, data: ByteArray) {
        val parsedValue = when {
            uuid.contains("2A1C") -> {
                val raw = BleDataParser.parseTemperatureRaw(data)
                val v = if (raw in 30.0..45.0) raw else BleDataParser.parseTemperature(data).toDouble()
                "%.1f".format(v)
            }
            uuid.contains("2A37") -> BleDataParser.parseHeartRate(data).toString()
            uuid.contains("2A35") -> {
                val bp = BleDataParser.parseBloodPressure(data)
                "${bp.first}/${bp.second}"
            }
            uuid.contains("2A5E") || uuid.contains("2A5F") ->
                BleDataParser.parseSpO2(data).toString()
            else -> BleDataParser.parseHeartRate(data).toString()
        }
        onReadingsReceived(listOf(Pair(uuid, parsedValue)))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun enableCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        indicate: Boolean,
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = if (indicate) byteArrayOf(0x02, 0x00)
                               else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun enableAllNotifiableCharacteristics(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                val props = characteristic.properties
                val canNotify   = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY)   != 0
                val canIndicate = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                if (canNotify || canIndicate) {
                    enableCharacteristic(gatt, characteristic, indicate = canIndicate)
                }
            }
        }
    }
}
