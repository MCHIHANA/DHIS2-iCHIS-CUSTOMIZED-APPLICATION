package org.dhis2.form.ui.sensor

object SensorStatusText {
    const val SCANNING = "Searching for sensor..."
    const val DIRECT_CONNECTING = "Connecting to sensor..."
    const val SAVED_DEVICE_CONNECTING = "Reconnecting to saved device..."
    const val RECONNECTING_DEVICE = "Reconnecting device..."
    const val RETAKING_MEASUREMENT = "Retaking measurement..."
    const val CONNECTED = "Sensor connected. Receiving data..."
    const val WAITING_FOR_DATA = "Waiting for measurement data..."
    const val RETAKE_HINT = "Reconnect sensor to retake measurement."
    const val NO_SENSOR_FOUND = "No sensor found. Try again."
    const val DATA_RECEIVED_PREFIX = "Data received:"

    fun dataReceived(value: String): String = "$DATA_RECEIVED_PREFIX $value"

    fun isCompleted(status: String?): Boolean =
        status?.startsWith(DATA_RECEIVED_PREFIX, ignoreCase = true) == true

    fun isConnected(status: String?): Boolean =
        status?.contains("connected", ignoreCase = true) == true

    fun isActiveWorkflow(status: String?): Boolean =
        status == SCANNING ||
            status == DIRECT_CONNECTING ||
            status == SAVED_DEVICE_CONNECTING ||
            status == RECONNECTING_DEVICE ||
            status == RETAKING_MEASUREMENT ||
            status == WAITING_FOR_DATA

    fun isFailure(status: String?): Boolean =
        status == NO_SENSOR_FOUND ||
            status?.contains("denied", ignoreCase = true) == true ||
            status?.startsWith("Error:", ignoreCase = true) == true
}
