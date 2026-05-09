package org.dhis2.sensor.ble

import android.util.Log

private const val TAG = "BLE_PARSER"
private const val TAG_BP = "BLE_BP"

/**
 * BLE medical device data parser supporting multiple sensor types.
 *
 * Implements parsers for:
 * - Blood Pressure (0x2A35) with IEEE-11073 SFLOAT
 * - Temperature (0x2A1C) with IEEE-11073 FLOAT
 * - Heart Rate (0x2A37)
 * - SpO2 (0x2A5E, 0x2A5F)
 *
 * All parsers follow Bluetooth SIG GATT specifications.
 */
object BleDataParser {

    // ── Blood Pressure Measurement (0x2A35) ──────────────────────────────────

    /**
     * Parses a Blood Pressure Measurement characteristic (0x2A35) according to
     * Bluetooth SIG Blood Pressure Profile specification.
     *
     * **Packet Structure:**
     * ```
     * Byte 0:      Flags
     *              bit 0: Units (0=mmHg, 1=kPa)
     *              bit 1: Timestamp present
     *              bit 2: Pulse rate present
     *              bit 3: User ID present
     *              bit 4: Measurement status present
     * Bytes 1-2:   Systolic (IEEE-11073 16-bit SFLOAT)
     * Bytes 3-4:   Diastolic (IEEE-11073 16-bit SFLOAT)
     * Bytes 5-6:   MAP - Mean Arterial Pressure (IEEE-11073 16-bit SFLOAT)
     * Bytes 7+:    Optional fields (timestamp, pulse rate, user ID, status)
     * ```
     *
     * **IEEE-11073 16-bit SFLOAT Format:**
     * - Bits 0-11:  12-bit signed mantissa (two's complement)
     * - Bits 12-15: 4-bit signed exponent (two's complement)
     * - Value = mantissa × 10^exponent
     *
     * **Special Values:**
     * - 0x07FF: NaN (Not a Number)
     * - 0x0800: NRes (Not at this resolution)
     * - 0x07FE: +INFINITY
     * - 0x0802: -INFINITY
     * - 0x0801: Reserved
     *
     * @return BloodPressureReading with systolic, diastolic, MAP, and optional pulse rate
     */
    fun parseBloodPressure(data: ByteArray): BloodPressureReading {
        Log.d(TAG_BP, "=== Blood Pressure Packet (${data.size} bytes) ===")
        Log.d(TAG_BP, "Raw: ${data.joinToString(" ") { "%02X".format(it) }}")

        if (data.size < 7) {
            Log.w(TAG_BP, "Packet too short (${data.size} bytes, need at least 7)")
            return BloodPressureReading(0f, 0f, 0f, null)
        }

        // Byte 0: Flags
        val flags = data[0].toInt() and 0xFF
        val unitsKpa = (flags and 0x01) != 0
        val timestampPresent = (flags and 0x02) != 0
        val pulseRatePresent = (flags and 0x04) != 0
        val userIdPresent = (flags and 0x08) != 0
        val statusPresent = (flags and 0x10) != 0

        Log.d(TAG_BP, "Flags: 0x${flags.toString(16).uppercase()}")
        Log.d(TAG_BP, "  Units: ${if (unitsKpa) "kPa" else "mmHg"}")
        Log.d(TAG_BP, "  Timestamp: $timestampPresent")
        Log.d(TAG_BP, "  Pulse Rate: $pulseRatePresent")
        Log.d(TAG_BP, "  User ID: $userIdPresent")
        Log.d(TAG_BP, "  Status: $statusPresent")

        // Bytes 1-2: Systolic (SFLOAT)
        val systolic = parseSFloat(data, 1)
        Log.d(TAG_BP, "Systolic: $systolic ${if (unitsKpa) "kPa" else "mmHg"}")

        // Bytes 3-4: Diastolic (SFLOAT)
        val diastolic = parseSFloat(data, 3)
        Log.d(TAG_BP, "Diastolic: $diastolic ${if (unitsKpa) "kPa" else "mmHg"}")

        // Bytes 5-6: MAP (SFLOAT)
        val map = parseSFloat(data, 5)
        Log.d(TAG_BP, "MAP: $map ${if (unitsKpa) "kPa" else "mmHg"}")

        // Parse optional fields
        var offset = 7
        var pulseRate: Float? = null

        // Skip timestamp if present (7 bytes: year, month, day, hour, minute, second, 0x00)
        if (timestampPresent) {
            if (data.size >= offset + 7) {
                offset += 7
                Log.d(TAG_BP, "Skipped timestamp (7 bytes)")
            }
        }

        // Parse pulse rate if present (SFLOAT, 2 bytes)
        if (pulseRatePresent) {
            if (data.size >= offset + 2) {
                pulseRate = parseSFloat(data, offset)
                Log.d(TAG_BP, "Pulse Rate: $pulseRate bpm")
                offset += 2
            }
        }

        // Skip user ID if present (1 byte)
        if (userIdPresent) {
            if (data.size >= offset + 1) {
                offset += 1
                Log.d(TAG_BP, "Skipped user ID (1 byte)")
            }
        }

        // Skip measurement status if present (2 bytes)
        if (statusPresent) {
            if (data.size >= offset + 2) {
                Log.d(TAG_BP, "Skipped measurement status (2 bytes)")
            }
        }

        // Convert kPa to mmHg if needed (1 kPa = 7.50062 mmHg)
        val systolicMmHg = if (unitsKpa) systolic * 7.50062f else systolic
        val diastolicMmHg = if (unitsKpa) diastolic * 7.50062f else diastolic
        val mapMmHg = if (unitsKpa) map * 7.50062f else map

        Log.d(TAG_BP, "=== Final Values (mmHg) ===")
        Log.d(TAG_BP, "Systolic: $systolicMmHg mmHg")
        Log.d(TAG_BP, "Diastolic: $diastolicMmHg mmHg")
        Log.d(TAG_BP, "MAP: $mapMmHg mmHg")
        if (pulseRate != null) Log.d(TAG_BP, "Pulse: $pulseRate bpm")

        return BloodPressureReading(systolicMmHg, diastolicMmHg, mapMmHg, pulseRate)
    }

    /**
     * Parses an IEEE-11073 16-bit SFLOAT value from a byte array.
     *
     * **Format:**
     * - Bits 0-11:  12-bit signed mantissa (two's complement)
     * - Bits 12-15: 4-bit signed exponent (two's complement)
     * - Value = mantissa × 10^exponent
     *
     * @param data Byte array containing the SFLOAT
     * @param offset Starting byte index (little-endian, 2 bytes)
     * @return Parsed float value, or 0f for special values (NaN, infinity, etc.)
     */
    private fun parseSFloat(data: ByteArray, offset: Int): Float {
        if (data.size < offset + 2) {
            Log.w(TAG_BP, "Not enough bytes for SFLOAT at offset $offset")
            return 0f
        }

        // Read 16-bit little-endian value
        val raw = (data[offset].toInt() and 0xFF) or
                  ((data[offset + 1].toInt() and 0xFF) shl 8)

        // Extract mantissa (bits 0-11, 12-bit signed)
        val mantissaRaw = raw and 0x0FFF
        val mantissa = if (mantissaRaw and 0x0800 != 0) {
            // Negative: sign-extend from 12-bit to 32-bit
            mantissaRaw or -0x1000
        } else {
            mantissaRaw
        }

        // Extract exponent (bits 12-15, 4-bit signed)
        val exponentRaw = (raw shr 12) and 0x0F
        val exponent = if (exponentRaw and 0x08 != 0) {
            // Negative: sign-extend from 4-bit to 32-bit
            exponentRaw or -0x10
        } else {
            exponentRaw
        }

        // Check for special values
        when (mantissa) {
            0x07FF -> {
                Log.d(TAG_BP, "SFLOAT special value: NaN")
                return 0f
            }
            0x0800 -> {
                Log.d(TAG_BP, "SFLOAT special value: NRes")
                return 0f
            }
            0x07FE -> {
                Log.d(TAG_BP, "SFLOAT special value: +INFINITY")
                return Float.POSITIVE_INFINITY
            }
            -0x07FE -> {
                Log.d(TAG_BP, "SFLOAT special value: -INFINITY")
                return Float.NEGATIVE_INFINITY
            }
        }

        // Calculate value = mantissa × 10^exponent
        val value = mantissa * Math.pow(10.0, exponent.toDouble()).toFloat()
        Log.d(TAG_BP, "SFLOAT[offset=$offset]: raw=0x${raw.toString(16).uppercase()}, " +
                      "mantissa=$mantissa, exponent=$exponent, value=$value")
        return value
    }

    // ── Temperature Measurement (0x2A1C) ─────────────────────────────────────

    fun parseHeartRate(data: ByteArray): Int {
        if (data.size < 2) return 0
        return data[1].toInt() and 0xFF
    }

    /**
     * Parses a BLE Temperature Measurement characteristic (0x2A1C).
     *
     * Format per Bluetooth SIG Health Thermometer Profile spec:
     *   Byte 0      — flags
     *   Bytes 1–4   — temperature as IEEE-11073 32-bit FLOAT, little-endian
     *                 Bytes 1–3: 24-bit signed mantissa
     *                 Byte 4:    8-bit signed exponent (power of 10)
     *
     * Value = mantissa × 10^exponent
     *
     * Example: 36.7 °C is encoded as mantissa=367, exponent=-1 → 367 × 10⁻¹ = 36.7
     */
    fun parseTemperature(data: ByteArray): Float {
        if (data.size < 5) return 0f

        // 24-bit signed mantissa (little-endian, bytes 1–3)
        val rawMantissa = (data[1].toInt() and 0xFF) or
            ((data[2].toInt() and 0xFF) shl 8) or
            ((data[3].toInt() and 0xFF) shl 16)
        // Sign-extend from 24-bit to 32-bit
        val mantissa = if (rawMantissa and 0x800000 != 0) rawMantissa or -0x1000000 else rawMantissa

        // 8-bit signed exponent (byte 4)
        val exponent = data[4].toInt().toByte().toInt()

        val temperature = mantissa * Math.pow(10.0, exponent.toDouble())
        Log.d(TAG, "IEEE-11073 FLOAT → mantissa=$mantissa, exponent=$exponent, temp=$temperature °C")
        return temperature.toFloat()
    }

    /**
     * Alternative raw parse used in [onCharacteristicChanged] when the device
     * sends a simplified 2-byte little-endian value (tempRaw / 100.0).
     * Some thermometers use this simpler encoding instead of IEEE-11073.
     */
    fun parseTemperatureRaw(data: ByteArray): Double {
        if (data.size < 3) return 0.0
        val tempRaw = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        val temperature = tempRaw / 100.0
        Log.d(TAG, "Raw → tempRaw=$tempRaw, temp=$temperature °C")
        return temperature
    }

    // ── SpO2 Measurement ─────────────────────────────────────────────────────

    fun parseSpO2(data: ByteArray): Int {
        if (data.size < 2) return 0
        return data[1].toInt() and 0xFF
    }

    // ── Advertisement packet parser (kept as fallback) ───────────────────────

    /**
     * Attempts to extract a temperature from raw manufacturer-specific
     * advertisement bytes. Tries three common byte layouts and returns the
     * first result in the plausible human-body range (30–45 °C).
     * All raw bytes are logged under BLE_RAW for field debugging.
     *
     * @return temperature in °C, or `null` if no plausible value was found.
     */
    fun parseAdvertisementTemperature(bytes: ByteArray): Double? {
        Log.d("BLE_RAW", "Manufacturer data (${bytes.size} bytes): " +
            bytes.joinToString { "%02X".format(it) })

        if (bytes.size < 4) return null

        return tryLayout(bytes, 2, 10.0, "A")
            ?: tryLayout(bytes, 1, 10.0, "B")
            ?: if (bytes.size >= 5) tryLayout(bytes, 3, 100.0, "C") else null
    }

    private fun tryLayout(bytes: ByteArray, byteIndex: Int, divisor: Double, label: String): Double? {
        if (bytes.size < byteIndex + 2) return null
        val raw = (bytes[byteIndex].toInt() and 0xFF) or
            ((bytes[byteIndex + 1].toInt() and 0xFF) shl 8)
        val temp = raw / divisor
        Log.d(TAG, "Layout $label → raw=$raw, temp=${"%.1f".format(temp)} °C")
        return if (temp in 30.0..45.0) temp else null
    }
}

/**
 * Blood pressure measurement result.
 *
 * @param systolic Systolic pressure in mmHg
 * @param diastolic Diastolic pressure in mmHg
 * @param map Mean Arterial Pressure in mmHg
 * @param pulseRate Optional pulse rate in bpm
 */
data class BloodPressureReading(
    val systolic: Float,
    val diastolic: Float,
    val map: Float,
    val pulseRate: Float?
)
