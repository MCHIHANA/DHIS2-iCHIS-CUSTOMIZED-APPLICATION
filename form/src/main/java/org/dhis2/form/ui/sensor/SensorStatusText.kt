package org.dhis2.form.ui.sensor

object SensorStatusText {
    const val SCANNING = "Searching for sensor..."
    const val DIRECT_CONNECTING = "Connecting to sensor..."
    const val CONNECTED = "Sensor connected. Receiving data..."
    const val WAITING_FOR_DATA = "Waiting for measurement data..."
    const val RETAKE_HINT = "Reconnect sensor to retake measurement."
    const val NO_SENSOR_FOUND = "No sensor found. Try again."
    const val DATA_RECEIVED_PREFIX = "Data received:"

    fun dataReceived(value: String): String = "$DATA_RECEIVED_PREFIX $value"
}
