package org.dhis2.form.ui.sensor

import android.util.Log
import kotlinx.coroutines.delay
import org.dhis2.form.ui.SensorType

object SensorConnectionManager {

    suspend fun scan(type: SensorType): String? {
        Log.d("Sensor", "Scanning via ${type}...")
        delay(2000)
        return when (type) {
            SensorType.TEMPERATURE -> "36.7"
            SensorType.WEIGHT -> "65"
            SensorType.HEART_RATE -> "72"
            SensorType.SYSTOLIC -> "120"
            SensorType.DIASTOLIC -> "80"
            else -> "36.5"
        }
    }

    suspend fun scanBluetooth(): String? {
        Log.d("Sensor", "Scanning Bluetooth...")
        delay(2000)
        return "36.7"
    }

    suspend fun scanUsb(): String? {
        Log.d("Sensor", "Scanning USB...")
        delay(2000)
        return "36.5"
    }

    suspend fun scanWifi(): String? {
        Log.d("Sensor", "Scanning WiFi...")
        delay(2000)
        return "36.9"
    }
}
