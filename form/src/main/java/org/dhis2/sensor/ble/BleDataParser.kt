package org.dhis2.sensor.ble

object BleDataParser {
    fun parseHeartRate(data: ByteArray): Int {
        if (data.size < 2) return 0
        return data[1].toInt() and 0xFF // Ensure unsigned byte interpretation
    }

    fun parseTemperature(data: ByteArray): Float {
        if (data.size < 2) return 0f
        return data[1].toFloat() // Assuming simplified raw format
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
}
