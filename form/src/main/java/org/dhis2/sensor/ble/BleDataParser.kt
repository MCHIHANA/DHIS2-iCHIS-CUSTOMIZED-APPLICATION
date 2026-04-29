package org.dhis2.sensor.ble

import android.util.Log

private const val TAG = "BLE_TEMP"

object BleDataParser {

    // ── GATT characteristic parsers ──────────────────────────────────────────

    fun parseHeartRate(data: ByteArray): Int {
        if (data.size < 2) return 0
        return data[1].toInt() and 0xFF
    }

    /** Parse a GATT Temperature Measurement characteristic (IEEE-11073 float, 0x2A1C). */
    fun parseTemperature(data: ByteArray): Float {
        if (data.size < 5) return 0f
        // Byte 0 = flags, bytes 1-4 = IEEE-11073 32-bit FLOAT (little-endian)
        // Mantissa = bytes[1..3] (24-bit signed), Exponent = bytes[4] (8-bit signed)
        val mantissa = (data[1].toInt() and 0xFF) or
            ((data[2].toInt() and 0xFF) shl 8) or
            ((data[3].toInt() and 0xFF) shl 16)
        val exponent = data[4].toInt()
        return (mantissa * Math.pow(10.0, exponent.toDouble())).toFloat()
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

    // ── Advertisement packet parser ──────────────────────────────────────────

    /**
     * Attempts to extract a temperature reading from raw manufacturer-specific
     * advertisement bytes broadcast by the FORA IR42 (and similar FORA sensors).
     *
     * The FORA IR42 is not publicly documented, so we try the three most common
     * byte layouts seen in BLE thermometer advertisements and pick the first result
     * that falls in a plausible human-body range (30–45 °C).
     *
     * All raw bytes are logged under BLE_RAW so you can verify the actual layout
     * on your specific firmware version and adjust if needed.
     *
     * @return temperature in °C, or `null` if no plausible value was found.
     */
    fun parseAdvertisementTemperature(bytes: ByteArray): Double? {
        Log.d("BLE_RAW", "Manufacturer data (${bytes.size} bytes): ${bytes.joinToString { "%02X".format(it) }}")

        if (bytes.size < 4) {
            Log.d(TAG, "Payload too short (${bytes.size} bytes) — skipping")
            return null
        }

        // Layout A — bytes[2..3] little-endian, value / 10
        // Common in FORA and Beurer BLE thermometers
        val layoutA = tryLayout(bytes, byteIndex = 2, shift = 0, divisor = 10.0, label = "A")
        if (layoutA != null) return layoutA

        // Layout B — bytes[1..2] little-endian, value / 10
        val layoutB = tryLayout(bytes, byteIndex = 1, shift = 0, divisor = 10.0, label = "B")
        if (layoutB != null) return layoutB

        // Layout C — bytes[3..4] little-endian, value / 100
        // Some devices encode as 3670 → 36.70 °C
        if (bytes.size >= 5) {
            val layoutC = tryLayout(bytes, byteIndex = 3, shift = 0, divisor = 100.0, label = "C")
            if (layoutC != null) return layoutC
        }

        Log.d(TAG, "No plausible temperature found in advertisement payload")
        return null
    }

    /**
     * Reads a little-endian 16-bit unsigned value starting at [byteIndex],
     * divides by [divisor], and returns it only if it's in the plausible
     * human-body range 30–45 °C.
     */
    private fun tryLayout(
        bytes: ByteArray,
        byteIndex: Int,
        shift: Int,
        divisor: Double,
        label: String,
    ): Double? {
        if (bytes.size < byteIndex + 2) return null
        val raw = ((bytes[byteIndex].toInt() and 0xFF) or
            ((bytes[byteIndex + 1].toInt() and 0xFF) shl 8)) ushr shift
        val temp = raw / divisor
        Log.d(TAG, "Layout $label → raw=$raw, temp=${"%.1f".format(temp)} °C")
        return if (temp in 30.0..45.0) temp else null
    }
}
