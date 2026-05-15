package org.dhis2.sensors.utils

object ByteUtils {
    fun formatBytes(data: ByteArray): String = data.joinToString(" ") { "%02X".format(it) }

    fun readUInt16LittleEndian(
        data: ByteArray,
        offset: Int,
    ): Int {
        if (data.size < offset + 2) {
            return 0
        }
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun parseSFloat(
        data: ByteArray,
        offset: Int,
    ): Float {
        val raw = readUInt16LittleEndian(data, offset)
        val mantissaRaw = raw and 0x0FFF
        val mantissa = if (mantissaRaw and 0x0800 != 0) {
            mantissaRaw or -0x1000
        } else {
            mantissaRaw
        }
        val exponentRaw = (raw shr 12) and 0x0F
        val exponent = if (exponentRaw and 0x08 != 0) {
            exponentRaw or -0x10
        } else {
            exponentRaw
        }

        return when (mantissa) {
            0x07FF, 0x0800 -> 0f
            0x07FE -> Float.POSITIVE_INFINITY
            -0x07FE -> Float.NEGATIVE_INFINITY
            else -> mantissa * Math.pow(10.0, exponent.toDouble()).toFloat()
        }
    }

    fun parseFloat11073(
        data: ByteArray,
        offset: Int = 1,
    ): Float {
        if (data.size < offset + 4) {
            return 0f
        }

        val rawMantissa = (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16)
        val mantissa = if (rawMantissa and 0x800000 != 0) {
            rawMantissa or -0x1000000
        } else {
            rawMantissa
        }
        val exponent = data[offset + 3].toInt().toByte().toInt()
        return (mantissa * Math.pow(10.0, exponent.toDouble())).toFloat()
    }
}
