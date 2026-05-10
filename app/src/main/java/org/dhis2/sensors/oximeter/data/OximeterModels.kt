package org.dhis2.sensors.oximeter.data

/**
 * Represents a single oximeter reading from the FORA O2 device.
 *
 * @property spo2 SpO2 percentage (0-100)
 * @property heartRateBpm Heart rate in beats per minute
 * @property timestamp Unix timestamp when the reading was captured
 */
data class OximeterReading(
    val spo2: Int,
    val heartRateBpm: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this reading is stable compared to another reading.
     * Readings are considered stable if both values are within ±2 units.
     */
    fun isStableWith(other: OximeterReading): Boolean {
        return kotlin.math.abs(spo2 - other.spo2) <= 2 &&
                kotlin.math.abs(heartRateBpm - other.heartRateBpm) <= 2
    }
}

/**
 * Represents the complete state of the oximeter feature.
 *
 * @property connectionStatus Current BLE connection status
 * @property latestReading Most recent reading from the device
 * @property previousReading Previous reading for stability check
 * @property submissionStatus Status of DHIS2 submission
 * @property deviceInfo Information about the connected device
 * @property errorMessage Error message if any operation failed
 */
data class OximeterState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val latestReading: OximeterReading? = null,
    val previousReading: OximeterReading? = null,
    val submissionStatus: SubmissionStatus = SubmissionStatus.IDLE,
    val deviceInfo: DeviceInfo? = null,
    val errorMessage: String? = null
) {
    /**
     * Checks if readings are stable enough for submission.
     * Requires both current and previous readings to be within ±2 units.
     */
    val hasStableReading: Boolean
        get() = latestReading != null && 
                previousReading != null && 
                latestReading.isStableWith(previousReading)
    
    /**
     * Checks if submission button should be enabled.
     */
    val canSubmit: Boolean
        get() = hasStableReading && 
                submissionStatus != SubmissionStatus.SUBMITTING &&
                connectionStatus == ConnectionStatus.CONNECTED
}

/**
 * Represents the BLE connection status.
 */
enum class ConnectionStatus {
    /** Not connected, not scanning */
    IDLE,
    
    /** Actively scanning for devices */
    SCANNING,
    
    /** Attempting to connect to device */
    CONNECTING,
    
    /** Bonding/pairing with device */
    BONDING,
    
    /** Successfully connected and receiving data */
    CONNECTED,
    
    /** Disconnected from device */
    DISCONNECTED,
    
    /** Error occurred during connection or operation */
    ERROR
}

/**
 * Represents the status of DHIS2 data submission.
 */
enum class SubmissionStatus {
    /** No submission in progress */
    IDLE,
    
    /** Waiting for user confirmation */
    CONFIRMING,
    
    /** Submitting data to DHIS2 */
    SUBMITTING,
    
    /** Successfully submitted */
    SUCCESS,
    
    /** Submission failed */
    FAILED
}

/**
 * Information about the connected BLE device.
 *
 * @property manufacturer Device manufacturer name
 * @property model Device model name
 * @property firmwareVersion Firmware version string
 * @property serialNumber Device serial number
 * @property macAddress Device MAC address
 */
data class DeviceInfo(
    val manufacturer: String = "FORA",
    val model: String = "O2 Pulse Oximeter",
    val firmwareVersion: String = "Unknown",
    val serialNumber: String = "Unknown",
    val macAddress: String = "C0:26:DA:17:D5:7D"
)
