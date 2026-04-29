package org.dhis2.sensor.ble

import android.util.Log

private const val TAG = "BLE_TEMP"

object BleDataParser {

    // ── GATT characteristic parsers ──────────────────────────────────────────

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
        Log.d(TAG, "IEEE-11073 → mantissa=$mantissa, exponent=$exponent, temp=$temperature °C")
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

    fun parseBloodPressure(data: ByteArray): Pair<Int, Int> {
        if (data.size < 3) return Pair(0, 0)
        val systolic = data[1].toInt() and 0xFF
        val diastolic = data[2].toInt() and 0xFF
        return Pair(systolic, diastolic)
    }

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
