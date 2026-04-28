package org.dhis2.form.ui.sensor.ble

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
        // Blood Pressure Measurement Compound Value (Systolic, Diastolic)
        // Values are in mmHg (SFLOAT)
        val systolic = parseSFloat(data[1], data[2]).toInt()
        val diastolic = parseSFloat(data[3], data[4]).toInt()
        return Pair(systolic, diastolic)
    }

    fun parseSpO2(data: ByteArray): Int {
        if (data.size < 3) return 0
        // Standard PLX Continuous Measurement
        return parseSFloat(data[1], data[2]).toInt()
    }

    private fun parseSFloat(b1: Byte, b2: Byte): Float {
        val mantissa = ((b1.toInt() and 0xFF) or ((b2.toInt() and 0x0F) shl 8))
        val exponent = (b2.toInt() shr 4)
        
        val signedMantissa = if (mantissa > 2047) mantissa - 4096 else mantissa
        val signedExponent = if (exponent > 7) exponent - 16 else exponent
        
        return signedMantissa.toFloat() * 10.0f.pow(signedExponent.toFloat())
    }
}
