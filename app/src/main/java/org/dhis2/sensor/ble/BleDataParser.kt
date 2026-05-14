package org.dhis2.sensor.ble

import android.util.Log
import kotlin.math.pow

object BleDataParser {

    fun parseTemperature(data: ByteArray): Float {
        if (data.size < 5) return 0f
        val flags = data[0].toInt()
        val isFahrenheit = (flags and 0x01) != 0
        
        // IEEE-11073 32-bit FLOAT
        val mantissa = (data[1].toInt() and 0xFF) or
                ((data[2].toInt() and 0xFF) shl 8) or
                ((data[3].toInt() and 0xFF) shl 16)
        val exponent = data[4].toInt()
        
        var temperature = mantissa.toFloat() * 10.0f.pow(exponent.toFloat())
        
        if (isFahrenheit) {
            temperature = (temperature - 32) * 5 / 9
        }
        
        return temperature
    }

    fun parseHeartRate(data: ByteArray): Int {
        if (data.isEmpty()) return 0
        val flags = data[0].toInt()
        val is8Bit = (flags and 0x01) == 0
        return if (is8Bit) {
            if (data.size >= 2) data[1].toInt() and 0xFF else 0
        } else {
            if (data.size >= 3) {
                (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            } else 0
        }
    }

    fun parseBloodPressure(data: ByteArray): Pair<Int, Int> {
        if (data.size < 7) return Pair(0, 0)
        // Flags at data[0]
        // Blood Pressure Measurement Compound Value (Systolic, Diastolic, Mean Arterial Pressure)
        // Each is 16-bit SFLOAT
        val systolic = parseSFloat(data[1], data[2]).toInt()
        val diastolic = parseSFloat(data[3], data[4]).toInt()
        return Pair(systolic, diastolic)
    }

    fun parseSpO2(data: ByteArray): Int {
        if (data.size < 3) return 0
        // PLX Spot-Check Measurement (0x2A5E) or Continuous (0x2A5F)
        // Simplifying for common sensors
        val flags = data[0].toInt()
        val spo2 = parseSFloat(data[1], data[2]).toInt()
        return spo2
    }

    /**
     * Parse glucose measurement according to Bluetooth SIG Glucose Service specification.
     * 
     * Glucose Measurement Characteristic (0x2A18) structure:
     * - Byte 0: Flags
     *   bit 0: Time Offset Present
     *   bit 1: Glucose Concentration, Type and Sample Location Present
     *   bit 2: Glucose Concentration Units (0=kg/L, 1=mol/L)
     *   bit 3: Sensor Status Annunciation Present
     *   bit 4: Context Information Follows
     * - Bytes 1-2: Sequence Number (uint16)
     * - Bytes 3-9: Base Time (year, month, day, hours, minutes, seconds)
     * - Bytes 10-11: Time Offset (sint16, optional)
     * - Bytes 12-13: Glucose Concentration (SFLOAT)
     * - Byte 14: Type-Sample Location (optional)
     * - Bytes 15-16: Sensor Status Annunciation (optional)
     * 
     * @param data Raw byte array from BLE characteristic
     * @return GlucoseReading containing glucose value in mg/dL and additional metadata
     */
    fun parseGlucose(data: ByteArray): GlucoseReading {
        if (data.size < 10) {
            Log.w("BLE_GLUCOSE", "Invalid glucose packet size: ${data.size}")
            return GlucoseReading(0f, "mg/dL", 0, null, null)
        }

        Log.d("BLE_GLUCOSE", "=== Glucose Packet (${data.size} bytes) ===")
        Log.d("BLE_GLUCOSE", "Raw: ${data.joinToString(" ") { "%02X".format(it) }}")

        val flags = data[0].toInt() and 0xFF
        Log.d("BLE_GLUCOSE", "Flags: 0x${"%02X".format(flags)}")

        val timeOffsetPresent = (flags and 0x01) != 0
        val concentrationPresent = (flags and 0x02) != 0
        val unitsKgPerL = (flags and 0x04) == 0  // 0=kg/L (mg/dL), 1=mol/L (mmol/L)
        val statusPresent = (flags and 0x08) != 0
        val contextFollows = (flags and 0x10) != 0

        Log.d("BLE_GLUCOSE", "  Time Offset Present: $timeOffsetPresent")
        Log.d("BLE_GLUCOSE", "  Concentration Present: $concentrationPresent")
        Log.d("BLE_GLUCOSE", "  Units: ${if (unitsKgPerL) "mg/dL" else "mmol/L"}")
        Log.d("BLE_GLUCOSE", "  Status Present: $statusPresent")
        Log.d("BLE_GLUCOSE", "  Context Follows: $contextFollows")

        // Sequence number (uint16)
        val sequenceNumber = ((data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8))
        Log.d("BLE_GLUCOSE", "Sequence Number: $sequenceNumber")

        // Base time (7 bytes)
        val year = ((data[3].toInt() and 0xFF) or ((data[4].toInt() and 0xFF) shl 8))
        val month = data[5].toInt() and 0xFF
        val day = data[6].toInt() and 0xFF
        val hours = data[7].toInt() and 0xFF
        val minutes = data[8].toInt() and 0xFF
        val seconds = data[9].toInt() and 0xFF

        val timestamp = String.format("%04d-%02d-%02d %02d:%02d:%02d", 
            year, month, day, hours, minutes, seconds)
        Log.d("BLE_GLUCOSE", "Timestamp: $timestamp")

        var offset = 10

        // Time offset (optional, sint16)
        var timeOffset: Int? = null
        if (timeOffsetPresent && data.size >= offset + 2) {
            timeOffset = ((data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8))
            // Sign extend if negative
            if (timeOffset and 0x8000 != 0) {
                timeOffset = timeOffset or -0x10000
            }
            Log.d("BLE_GLUCOSE", "Time Offset: $timeOffset minutes")
            offset += 2
        }

        // Glucose concentration (SFLOAT)
        var glucoseValue = 0f
        var unit = if (unitsKgPerL) "mg/dL" else "mmol/L"

        if (concentrationPresent && data.size >= offset + 2) {
            glucoseValue = parseSFloat(data[offset], data[offset + 1])
            Log.d("BLE_GLUCOSE", "SFLOAT[offset=$offset]: raw=0x${"%02X%02X".format(data[offset + 1], data[offset])}, value=$glucoseValue")
            
            // Convert mmol/L to mg/dL if needed (1 mmol/L = 18.0182 mg/dL)
            if (!unitsKgPerL) {
                val mgdl = glucoseValue * 18.0182f
                Log.d("BLE_GLUCOSE", "Converting $glucoseValue mmol/L to $mgdl mg/dL")
                glucoseValue = mgdl
                unit = "mg/dL"
            }
            
            offset += 2
        }

        // Type-Sample Location (optional)
        var typeSampleLocation: String? = null
        if (concentrationPresent && data.size >= offset + 1) {
            val typeSample = data[offset].toInt() and 0xFF
            val type = (typeSample and 0x0F)
            val sampleLocation = (typeSample shr 4) and 0x0F
            
            val typeStr = when (type) {
                1 -> "Capillary Whole blood"
                2 -> "Capillary Plasma"
                3 -> "Venous Whole blood"
                4 -> "Venous Plasma"
                5 -> "Arterial Whole blood"
                6 -> "Arterial Plasma"
                7 -> "Undetermined Whole blood"
                8 -> "Undetermined Plasma"
                9 -> "Interstitial Fluid (ISF)"
                10 -> "Control Solution"
                else -> "Reserved"
            }
            
            val locationStr = when (sampleLocation) {
                1 -> "Finger"
                2 -> "Alternate Site Test (AST)"
                3 -> "Earlobe"
                4 -> "Control solution"
                15 -> "Sample Location value not available"
                else -> "Reserved"
            }
            
            typeSampleLocation = "$typeStr, $locationStr"
            Log.d("BLE_GLUCOSE", "Type-Sample Location: $typeSampleLocation")
            offset += 1
        }

        // Sensor Status Annunciation (optional, 16-bit)
        if (statusPresent && data.size >= offset + 2) {
            val status = ((data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8))
            Log.d("BLE_GLUCOSE", "Sensor Status: 0x${"%04X".format(status)}")
            // Decode status bits if needed
        }

        Log.d("BLE_GLUCOSE", "✓ Valid glucose reading: $glucoseValue $unit")

        return GlucoseReading(
            value = glucoseValue,
            unit = unit,
            sequenceNumber = sequenceNumber,
            timestamp = timestamp,
            typeSampleLocation = typeSampleLocation
        )
    }

    /**
     * Data class representing a glucose measurement
     */
    data class GlucoseReading(
        val value: Float,
        val unit: String,
        val sequenceNumber: Int,
        val timestamp: String?,
        val typeSampleLocation: String?
    )

    private fun parseSFloat(b1: Byte, b2: Byte): Float {
        val mantissa = ((b1.toInt() and 0xFF) or ((b2.toInt() and 0x0F) shl 8))
        val exponent = (b2.toInt() shr 4)
        
        // Handle negative mantissa (12-bit signed)
        val signedMantissa = if (mantissa > 2047) mantissa - 4096 else mantissa
        // Handle negative exponent (4-bit signed)
        val signedExponent = if (exponent > 7) exponent - 16 else exponent
        
        return signedMantissa.toFloat() * 10.0f.pow(signedExponent.toFloat())
    }
}
