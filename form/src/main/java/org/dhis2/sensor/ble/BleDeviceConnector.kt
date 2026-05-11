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
private const val TAG_SPO2    = "BLE_SPO2"

// ── Standard BLE SIG UUIDs ────────────────────────────────────────────────────
private val TEMP_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
private val TEMP_CHAR_UUID    = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb")
private val CCCD_UUID         = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// ── FORA O2 Nordic service UUIDs ─────────────────────────────────────────────
private val NORDIC_SERVICE_UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
private val NORDIC_BUTTON_UUID  = UUID.fromString("00001524-1212-efde-1523-785feabcd123")

// ── Blood Pressure service UUIDs (Bluetooth SIG standard) ────────────────────
private val BP_SERVICE_UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")
private val BP_MEASUREMENT_UUID = UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb")

@SuppressLint("MissingPermission")
class BleDeviceConnector(
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onReadingsReceived: (List<Pair<String, String>>) -> Unit,
) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentSensorType: SensorType = SensorType.UNKNOWN

    /**
     * Handler used to schedule a retry trigger for the FORA O2 if the first
     * WRITE_WITHOUT_RESPONSE command does not result in notifications within 2 s.
     */
    private val retryHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var retryRunnable: Runnable? = null
    @Volatile private var spo2DataReceived = false

    private val connectHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var connectDevice: BluetoothDevice? = null
    private var connectContext: Context? = null
    private var connectRetries = 0
    private val maxConnectRetries = 2

    fun connect(context: Context, device: BluetoothDevice, sensorType: SensorType) {
        currentSensorType = sensorType
        connectDevice = device
        connectContext = context
        connectRetries = 0
        Log.d(TAG_CONNECT, "Connecting to ${device.address} (type=$sensorType)...")
        // autoConnect=false: direct connection — much faster than background autoConnect.
        // If we get GATT_ERROR(133) we retry up to maxConnectRetries times with a short delay.
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        connectHandler.removeCallbacksAndMessages(null)
        connectRetries = 0
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onConnectionStateChanged(false)
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when {
                newState == BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG_CONNECT, "Connected to ${gatt.device.address}")
                    onConnectionStateChanged(true)
                    // Small delay before discoverServices() — recommended by Android BLE docs
                    // to let the connection stabilise before starting service discovery.
                    connectHandler.postDelayed({ gatt.discoverServices() }, 300)
                }
                newState == BluetoothProfile.STATE_DISCONNECTED && status == 133 -> {
                    // GATT_ERROR 133 — device was found by scan but connection timed out.
                    // Retry with a short delay (device may still be advertising).
                    gatt.close()
                    if (connectRetries < maxConnectRetries) {
                        connectRetries++
                        Log.w(TAG_CONNECT, "GATT error 133 — retry $connectRetries/$maxConnectRetries in 500ms")
                        connectHandler.postDelayed({
                            val ctx = connectContext ?: return@postDelayed
                            val dev = connectDevice ?: return@postDelayed
                            bluetoothGatt = dev.connectGatt(ctx, false, gattCallback)
                        }, 500)
                    } else {
                        Log.e(TAG_CONNECT, "GATT error 133 — max retries reached, giving up")
                        onConnectionStateChanged(false)
                    }
                }
                newState == BluetoothProfile.STATE_DISCONNECTED -> {
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
                SensorType.BLOOD_PRESSURE -> subscribeBloodPressure(gatt)
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
            Log.d("BLE_RAW", "*** onCharacteristicChanged (API 33+) *** [$uuid] ${value.joinToString { "%02X".format(it) }}")
            Log.d("BLE_CALLBACK", "onCharacteristicChanged (API 33+) called for $uuid, sensorType=$currentSensorType")
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
            Log.d("BLE_RAW", "*** onCharacteristicChanged (legacy) *** [$uuid] ${data.joinToString { "%02X".format(it) }}")
            Log.d("BLE_CALLBACK", "onCharacteristicChanged (legacy) called for $uuid, sensorType=$currentSensorType")
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
            val charUuid = descriptor.characteristic.uuid
            Log.d(TAG_SERVICE, "*** onDescriptorWrite: $charUuid status=$status ***")
            Log.d(TAG_SERVICE, "Current sensor type: $currentSensorType")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG_SERVICE, "!!! Descriptor write FAILED (status=$status) for $charUuid !!!")
                return
            }

            Log.d(TAG_SERVICE, "✓ Descriptor write SUCCESS for $charUuid")

            // After enabling CCCD on the FORA O2 Nordic characteristic, send the
            // trigger command so the device starts streaming SpO2 + pulse data.
            if (currentSensorType == SensorType.SPO2 && charUuid == NORDIC_BUTTON_UUID) {
                Log.d(TAG_SERVICE, "*** Sending FORA O2 trigger command now ***")
                sendForaO2TriggerCommand(gatt, descriptor.characteristic)
            } else {
                Log.d(TAG_SERVICE, "Waiting for data from sensor...")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            val uuid = characteristic.uuid
            Log.d(TAG_SERVICE, "onCharacteristicWrite: $uuid status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG_SERVICE, "Characteristic write failed (status=$status) for $uuid — retrying with WRITE_TYPE_DEFAULT")
                // Retry once with the default write type in case WRITE_TYPE_NO_RESPONSE
                // is not supported by this firmware revision.
                val triggerCommand = byteArrayOf(0x01)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        characteristic,
                        triggerCommand,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = triggerCommand
                    @Suppress("DEPRECATION")
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(characteristic)
                }
            } else {
                Log.d(TAG_SERVICE, "Trigger write acknowledged — waiting for SpO2/pulse notifications")
            }
        }
    }

    // ── Dispatch to correct parser ────────────────────────────────────────────

    private fun dispatchReading(uuid: String, data: ByteArray) {
        when (currentSensorType) {
            SensorType.SPO2           -> handleForaO2Data(uuid, data)
            SensorType.TEMPERATURE    -> handleTemperatureData(uuid, data)
            SensorType.BLOOD_PRESSURE -> handleBloodPressureData(uuid, data)
            else                      -> handleGenericData(uuid, data)
        }
    }

    // ── FORA O2 ───────────────────────────────────────────────────────────────

    private fun subscribeForaO2(gatt: BluetoothGatt) {
        Log.d(TAG_SERVICE, "*** subscribeForaO2 called ***")
        val service = gatt.getService(NORDIC_SERVICE_UUID)
        if (service == null) {
            Log.w(TAG_SERVICE, "!!! Nordic service NOT FOUND — enabling all notifiable characteristics !!!")
            enableAllNotifiableCharacteristics(gatt)
            return
        }
        Log.d(TAG_SERVICE, "✓ Found Nordic service: ${service.uuid}")
        
        val buttonChar = service.getCharacteristic(NORDIC_BUTTON_UUID)
        if (buttonChar != null) {
            Log.d(TAG_SERVICE, "✓ Found FORA O2 characteristic: ${buttonChar.uuid}, properties=${buttonChar.properties}")
            val success = gatt.setCharacteristicNotification(buttonChar, true)
            Log.d(TAG_SERVICE, "setCharacteristicNotification result: $success")

            val descriptor = buttonChar.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                // Device has a CCCD — write ENABLE_NOTIFICATION_VALUE.
                // The trigger command will be sent from onDescriptorWrite().
                Log.d(TAG_SERVICE, "✓ Found CCCD descriptor, writing ENABLE_NOTIFICATION_VALUE")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val writeSuccess = gatt.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                    )
                    Log.d(TAG_SERVICE, "writeDescriptor (API 33+) result: $writeSuccess")
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    val writeSuccess = gatt.writeDescriptor(descriptor)
                    Log.d(TAG_SERVICE, "writeDescriptor (legacy) result: $writeSuccess")
                }
            } else {
                // FORA O2 has no CCCD on this characteristic — notifications are
                // enabled by setCharacteristicNotification() alone on the Android side.
                // We must send the trigger write immediately here because onDescriptorWrite
                // will never fire without a CCCD write.
                Log.d(TAG_SERVICE, "!!! No CCCD on ${buttonChar.uuid} — sending trigger directly !!!")
                sendForaO2TriggerCommand(gatt, buttonChar)
            }
            Log.d(TAG_SERVICE, "Subscribed to FORA O2 Nordic button characteristic (NOTIFY)")
        } else {
            Log.w(TAG_SERVICE, "!!! Nordic button characteristic NOT FOUND — enabling all !!!")
            enableAllNotifiableCharacteristics(gatt)
        }
    }

    /**
     * Writes a 0x01 trigger byte to the FORA O2 Nordic button characteristic to
     * instruct the device to start sending SpO2 + pulse notifications.
     *
     * Called either from [onDescriptorWrite] (when the device has a CCCD) or
     * directly from [subscribeForaO2] (when there is no CCCD to write).
     */
    /**
     * Sends the 0x01 start-measurement command to the FORA O2 Nordic characteristic.
     *
     * First attempt uses WRITE_WITHOUT_RESPONSE (matching the characteristic's props=0x18).
     * If no SpO2 data arrives within 2 seconds, [scheduleRetryTrigger] automatically
     * retries using WRITE_TYPE_DEFAULT (with ATT response) as a fallback.
     */
    private fun sendForaO2TriggerCommand(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        spo2DataReceived = false
        Log.d(TAG_SERVICE, "Sending trigger command (0x01) to FORA O2 [WRITE_WITHOUT_RESPONSE]...")
        val triggerCommand = byteArrayOf(0x01)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val writeSuccess = gatt.writeCharacteristic(
                characteristic,
                triggerCommand,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
            )
            Log.d(TAG_SERVICE, "Trigger command write (API 33+, NO_RESPONSE) result: $writeSuccess")
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = triggerCommand
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            val writeSuccess = gatt.writeCharacteristic(characteristic)
            Log.d(TAG_SERVICE, "Trigger command write (legacy, NO_RESPONSE) result: $writeSuccess")
        }
        // Schedule a retry with WRITE_TYPE_DEFAULT if no notifications arrive in 2 s
        scheduleRetryTrigger(gatt, characteristic)
    }

    /**
     * Posts a 2-second delayed runnable that retries the trigger with WRITE_TYPE_DEFAULT
     * if [spo2DataReceived] is still false (i.e. the first write produced no data).
     * Cancelled immediately when the first valid SpO2 packet arrives.
     */
    private fun scheduleRetryTrigger(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        retryRunnable?.let { retryHandler.removeCallbacks(it) }
        val runnable = Runnable {
            if (!spo2DataReceived) {
                Log.w(TAG_SERVICE, "No SpO2 data after 2s — retrying trigger with WRITE_TYPE_DEFAULT")
                val cmd = byteArrayOf(0x01)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        characteristic,
                        cmd,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = cmd
                    @Suppress("DEPRECATION")
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(characteristic)
                }
                Log.d(TAG_SERVICE, "Retry trigger (WRITE_TYPE_DEFAULT) sent")
            }
        }
        retryRunnable = runnable
        retryHandler.postDelayed(runnable, 2000L)
    }

    /**
     * Parses a FORA O2 data packet received from the Nordic button characteristic.
     *
     * FORA O2 packet layout (confirmed from device captures):
     *
     *   Byte 0 : flags / status
     *              bit7 = probe error
     *              bit6 = pulse searching
     *              bit4 = beep
     *   Byte 1 : SpO2 value  (0–100 %); 0x7F or 0xFF = no finger / initialising
     *   Byte 2 : Pulse rate low byte  (0xFF = invalid)
     *   Byte 3 : Pulse rate high byte (present in 4- and 5-byte packets)
     *   Byte 4 : Perfusion index (optional, 5-byte packet only)
     *
     * IMPORTANT: byte 0 is FLAGS, not SpO2. Never read SpO2 from byte 0.
     */
    private fun handleForaO2Data(uuid: String, data: ByteArray) {
        Log.d(TAG_SPO2, "FORA O2 raw (${data.size}B): ${data.joinToString { "%02X".format(it) }}")

        // Cancel any pending retry — we ARE receiving data
        if (!spo2DataReceived) {
            spo2DataReceived = true
            retryRunnable?.let { retryHandler.removeCallbacks(it) }
            Log.d(TAG_SPO2, "First data packet received — cancelling retry timer")
        }

        if (data.size < 3) {
            Log.d(TAG_SPO2, "Packet too short (${data.size}B) — skipping")
            return
        }

        // Byte 1 = SpO2; 0x7F (127) and 0xFF (255) mean no finger / still initialising
        val spo2Raw = data[1].toInt() and 0xFF
        if (spo2Raw == 0x7F || spo2Raw == 0xFF || spo2Raw == 0) {
            Log.d(TAG_SPO2, "SpO2=0x${spo2Raw.toString(16).uppercase()} — no finger or initialising")
            return
        }
        if (spo2Raw !in 50..100) {
            Log.d(TAG_SPO2, "SpO2 byte out of range ($spo2Raw) — discarding packet")
            return
        }
        val spo2 = spo2Raw

        // Bytes 2-3 = pulse rate (little-endian 16-bit)
        val pulseLo = data[2].toInt() and 0xFF
        val pulseHi = if (data.size > 3) data[3].toInt() and 0xFF else 0
        var pulse = pulseLo or (pulseHi shl 8)
        // 0x00FF or 0xFFFF means invalid reading
        if (pulseLo == 0xFF) {
            Log.d(TAG_SPO2, "Pulse rate invalid — no finger detected")
            return
        }
        // Sanity-check the combined value; fall back to low byte alone
        if (pulse !in 20..300) {
            Log.d(TAG_SPO2, "Pulse value $pulse out of range, using low byte only: $pulseLo")
            pulse = pulseLo
        }

        Log.d(TAG_SPO2, "✓ Valid reading: SpO2=$spo2% Pulse=$pulse bpm — emitting to ViewModel")
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

    // ── Blood Pressure ────────────────────────────────────────────────────────

    /**
     * Subscribes to Blood Pressure Measurement characteristic (0x2A35).
     * Uses INDICATE mode as per Bluetooth SIG Blood Pressure Profile specification.
     */
    private fun subscribeBloodPressure(gatt: BluetoothGatt) {
        Log.d(TAG_SERVICE, "*** subscribeBloodPressure called ***")
        val service = gatt.getService(BP_SERVICE_UUID)
        if (service == null) {
            Log.w(TAG_SERVICE, "!!! Blood Pressure service NOT FOUND — enabling all notifiable characteristics !!!")
            enableAllNotifiableCharacteristics(gatt)
            return
        }
        Log.d(TAG_SERVICE, "✓ Found Blood Pressure service: ${service.uuid}")
        
        val bpChar = service.getCharacteristic(BP_MEASUREMENT_UUID)
        if (bpChar != null) {
            Log.d(TAG_SERVICE, "✓ Found BP Measurement characteristic: ${bpChar.uuid}, properties=${bpChar.properties}")
            // Blood Pressure uses INDICATE mode (not NOTIFY)
            enableCharacteristic(gatt, bpChar, indicate = true)
            Log.d(TAG_SERVICE, "Subscribed to Blood Pressure Measurement (INDICATE)")
        } else {
            Log.w(TAG_SERVICE, "!!! BP Measurement characteristic NOT FOUND — enabling all !!!")
            enableAllNotifiableCharacteristics(gatt)
        }
    }

    /**
     * Parses Blood Pressure Measurement packets (0x2A35) and emits readings.
     *
     * The FORA D40b sends:
     * - Systolic pressure (mmHg)
     * - Diastolic pressure (mmHg)
     * - MAP (Mean Arterial Pressure)
     * - Pulse rate (bpm) - optional
     *
     * All values are parsed using IEEE-11073 16-bit SFLOAT format.
     * Readings are emitted as semantic keys: "SYSTOLIC", "DIASTOLIC", "PULSE"
     */
    private fun handleBloodPressureData(uuid: String, data: ByteArray) {
        Log.d("BLE_BP", "Blood Pressure raw (${data.size}B): ${data.joinToString { "%02X".format(it) }}")

        val reading = BleDataParser.parseBloodPressure(data)
        
        // Validate readings are in plausible range
        if (reading.systolic < 50f || reading.systolic > 250f ||
            reading.diastolic < 30f || reading.diastolic > 150f) {
            Log.w("BLE_BP", "Blood pressure values out of range — skipping")
            return
        }

        Log.d("BLE_BP", "✓ Valid BP reading: ${reading.systolic.toInt()}/${reading.diastolic.toInt()} mmHg")
        
        // Build readings list with semantic keys
        val readings = mutableListOf(
            Pair("SYSTOLIC", reading.systolic.toInt().toString()),
            Pair("DIASTOLIC", reading.diastolic.toInt().toString())
        )
        
        // Add pulse rate if present and valid
        reading.pulseRate?.let { pulse ->
            if (pulse in 30f..250f) {
                readings.add(Pair("PULSE", pulse.toInt().toString()))
                Log.d("BLE_BP", "  Pulse: ${pulse.toInt()} bpm")
            }
        }
        
        onReadingsReceived(readings)
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
                // Blood Pressure — return as multi-value readings
                val reading = BleDataParser.parseBloodPressure(data)
                val readings = mutableListOf(
                    Pair("SYSTOLIC", reading.systolic.toInt().toString()),
                    Pair("DIASTOLIC", reading.diastolic.toInt().toString())
                )
                reading.pulseRate?.let { pulse ->
                    if (pulse in 30f..250f) {
                        readings.add(Pair("PULSE", pulse.toInt().toString()))
                    }
                }
                onReadingsReceived(readings)
                return
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
