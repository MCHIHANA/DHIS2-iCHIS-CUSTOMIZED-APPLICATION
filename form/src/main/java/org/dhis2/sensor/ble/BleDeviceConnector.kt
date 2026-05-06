package org.dhis2.sensor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import java.util.UUID

private const val TAG_CONNECT = "BLE_CONNECT"
private const val TAG_SERVICE = "BLE_SERVICE"
private const val TAG_CHAR    = "BLE_CHAR"

// ── Standard BLE SIG UUIDs ────────────────────────────────────────────────────
private val TEMP_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
private val TEMP_CHAR_UUID    = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb")
private val CCCD_UUID         = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// ── FORA O2 Nordic service UUIDs ─────────────────────────────────────────────
private val NORDIC_SERVICE_UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
private val NORDIC_BUTTON_UUID  = UUID.fromString("00001524-1212-efde-1523-785feabcd123")

@SuppressLint("MissingPermission")
class BleDeviceConnector(
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onReadingsReceived: (List<Pair<String, String>>) -> Unit,
) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentSensorType: SensorType = SensorType.UNKNOWN

    fun connect(context: Context, device: BluetoothDevice, sensorType: SensorType) {
        currentSensorType = sensorType
        Log.d(TAG_CONNECT, "Connecting to ${device.address} (type=$sensorType)...")
        // autoConnect=true: Android will connect as soon as the device is reachable,
        // even if it powered off briefly. This avoids GATT_ERROR(133) race conditions
        // where the device stops advertising before connectGatt() completes.
        bluetoothGatt = device.connectGatt(context, true, gattCallback)
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
                    // Small delay before service discovery helps with some devices
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        gatt.discoverServices()
                    }, 300)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG_CONNECT, "Disconnected from ${gatt.device.address} (status=$status)")
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
                SensorType.SPO2        -> subscribeForaO2(gatt)
                SensorType.TEMPERATURE -> subscribeThermometer(gatt)
                else                   -> enableAllNotifiableCharacteristics(gatt)
            }
        }

        // ── Android 13+ (API 33) new signature ───────────────────────────────
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            val uuid = characteristic.uuid.toString().uppercase()
            Log.d("BLE_RAW", "[$uuid] ${value.joinToString { "%02X".format(it) }}")
            dispatchReading(uuid, value)
        }

        // ── Android 12 and below — deprecated but still needed ───────────────
        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            // On API 33+ this is never called; on API 32 and below it is.
            val data = characteristic.value ?: return
            val uuid = characteristic.uuid.toString().uppercase()
            Log.d("BLE_RAW", "[$uuid] ${data.joinToString { "%02X".format(it) }}")
            dispatchReading(uuid, data)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                val data = characteristic.value ?: return
                val uuid = characteristic.uuid.toString().uppercase()
                dispatchReading(uuid, data)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            Log.d(TAG_SERVICE, "Descriptor written: ${descriptor.characteristic.uuid} status=$status")
            // No trigger command needed — FORA O2 sends data automatically
            // once the finger is placed on the sensor after notifications are enabled.
        }
    }

    // ── Dispatch to correct parser ────────────────────────────────────────────

    private fun dispatchReading(uuid: String, data: ByteArray) {
        when (currentSensorType) {
            SensorType.SPO2        -> handleForaO2Data(uuid, data)
            SensorType.TEMPERATURE -> handleTemperatureData(uuid, data)
            else                   -> handleGenericData(uuid, data)
        }
    }

    // ── FORA O2 ───────────────────────────────────────────────────────────────

    private fun subscribeForaO2(gatt: BluetoothGatt) {
        val service = gatt.getService(NORDIC_SERVICE_UUID)
        if (service == null) {
            Log.w(TAG_SERVICE, "Nordic service not found — enabling all notifiable characteristics")
            enableAllNotifiableCharacteristics(gatt)
            return
        }
        val buttonChar = service.getCharacteristic(NORDIC_BUTTON_UUID)
        if (buttonChar != null) {
            enableCharacteristic(gatt, buttonChar, indicate = false)
            Log.d(TAG_SERVICE, "Subscribed to FORA O2 Nordic button characteristic (NOTIFY)")
        } else {
            Log.w(TAG_SERVICE, "Nordic button characteristic not found — enabling all")
            enableAllNotifiableCharacteristics(gatt)
        }
    }

    /**
     * Parses FORA O2 data packet.
     *
     * The FORA O2 sends packets continuously while a finger is detected.
     * Observed packet layouts (all little-endian):
     *
     *   Layout A (4 bytes): [flags, spo2, pulse_lo, pulse_hi]
     *   Layout B (5 bytes): [flags, spo2, pulse_lo, pulse_hi, perfusion_index]
     *
     * SpO2 range: 0–100. Values 0x7F (127) or 0xFF (255) = no finger / initialising.
     * Pulse range: 0–250 bpm typical. 0 or 0xFF = no reading.
     *
     * All raw bytes are logged under BLE_RAW for debugging.
     */
    private fun handleForaO2Data(uuid: String, data: ByteArray) {
        Log.d(TAG_SERVICE, "FORA O2 raw (${data.size}B): ${data.joinToString { "%02X".format(it) }}")

        if (data.size < 2) {
            Log.d(TAG_SERVICE, "Packet too short — skipping")
            return
        }

        // Try to find valid SpO2 in bytes 0–2 (different firmware versions use different offsets)
        var spo2 = -1
        var pulse = 0
        var byteOffset = -1

        for (i in 0 until minOf(data.size - 1, 3)) {
            val candidate = data[i].toInt() and 0xFF
            if (candidate in 50..100) {   // valid SpO2 range
                spo2 = candidate
                byteOffset = i
                // Pulse follows SpO2 as little-endian 16-bit
                pulse = (data.getOrNull(i + 1)?.toInt() ?: 0) and 0xFF
                if (data.size > i + 2) {
                    pulse = pulse or (((data[i + 2].toInt() and 0xFF) shl 8))
                }
                // Sanity check pulse
                if (pulse !in 20..300) pulse = pulse and 0xFF  // take just low byte
                break
            }
        }

        if (spo2 == -1) {
            Log.d(TAG_SERVICE, "No valid SpO2 in packet (no finger or initialising)")
            return
        }

        Log.d("BLE_SPO2", "SpO2=$spo2% Pulse=$pulse bpm (offset=$byteOffset)")

        onReadingsReceived(
            listOf(
                Pair("SPO2",  spo2.toString()),
                Pair("PULSE", pulse.toString()),
            ),
        )
    }

    // ── Thermometer ───────────────────────────────────────────────────────────

    private fun subscribeThermometer(gatt: BluetoothGatt) {
        val service = gatt.getService(TEMP_SERVICE_UUID)
        if (service == null) {
            Log.w(TAG_SERVICE, "Health Thermometer service not found — enabling all")
            enableAllNotifiableCharacteristics(gatt)
            return
        }
        val char = service.getCharacteristic(TEMP_CHAR_UUID)
        if (char != null) {
            enableCharacteristic(gatt, char, indicate = true)
            Log.d(TAG_SERVICE, "Subscribed to Temperature Measurement (INDICATE)")
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

    // ── Generic fallback ──────────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun enableCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        indicate: Boolean,
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+ — use new writeDescriptor API
                gatt.writeDescriptor(
                    descriptor,
                    if (indicate) byteArrayOf(0x02, 0x00)
                    else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                )
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = if (indicate) byteArrayOf(0x02, 0x00)
                                   else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        } else {
            Log.w(TAG_SERVICE, "No CCCD on ${characteristic.uuid} — notifications may not work")
        }
    }

    private fun enableAllNotifiableCharacteristics(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                val props = characteristic.properties
                val canNotify   = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY)   != 0
                val canIndicate = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                if (canNotify || canIndicate) {
                    enableCharacteristic(gatt, characteristic, indicate = canIndicate && !canNotify)
                }
            }
        }
    }
}
