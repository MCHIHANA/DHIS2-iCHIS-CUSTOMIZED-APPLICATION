package org.dhis2.sensors.core

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dhis2.sensors.utils.SensorLogger

class BleManager(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bleScanner = BleScanner(context)
    private val deviceCache = BleDeviceCache(context)
    private val bleConnector = BleConnector(
        onConnectionStateChanged = { isConnected ->
            _connectionState.value = if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
            if (isConnected) {
                directReconnectJob?.cancel()
                currentDevice?.let { device ->
                    deviceCache.put(currentSensorType, device.address)
                }
                pendingDirectReconnect = null
            } else if (pendingDirectReconnect != null) {
                fallbackToDiscoveryScan("direct connection closed before data session")
            }

            if (!isConnected) {
                isConnecting = false
            }
            SensorLogger.d(TAG, "Connection state changed: ${_connectionState.value}")
        },
        onReadingsReceived = { readings ->
            SensorLogger.d(TAG, "Readings received from device: ${readings.size}")
            _sensorData.value = readings
        },
    )

    @Volatile
    private var isConnecting = false
    private var currentSensorType: SensorType = SensorType.UNKNOWN
    private var currentDevice: BluetoothDevice? = null
    private var pendingDirectReconnect: DirectReconnectRequest? = null
    private var directReconnectJob: Job? = null
    private var discoveryRetryJob: Job? = null

    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<List<SensorReading>>(emptyList())
    val sensorData: StateFlow<List<SensorReading>> = _sensorData.asStateFlow()

    private val _currentDeviceAddress = MutableStateFlow<String?>(null)
    val currentDeviceAddress: StateFlow<String?> = _currentDeviceAddress.asStateFlow()

    private val _currentDeviceName = MutableStateFlow<String?>(null)
    val currentDeviceName: StateFlow<String?> = _currentDeviceName.asStateFlow()

    fun startScan(
        preferredDeviceAddress: String? = null,
        preferredSensorType: SensorType = SensorType.UNKNOWN,
    ) {
        _devices.value = emptyList()
        _sensorData.value = emptyList()
        isConnecting = false
        discoveryRetryJob?.cancel()
        currentSensorType = preferredSensorType
        SensorLogger.d(TAG, "Starting BLE workflow (preferredMac=$preferredDeviceAddress, sensorType=$preferredSensorType)")

        val cachedAddress =
            preferredDeviceAddress?.uppercase()
                ?: deviceCache.get(preferredSensorType)

        if (cachedAddress != null && tryDirectReconnect(cachedAddress, preferredSensorType)) {
            return
        }

        startDiscoveryScan(
            preferredAddress = cachedAddress,
            preferredSensorType = preferredSensorType,
        )
    }

    fun stopScan() {
        bleScanner.stopScan()
        directReconnectJob?.cancel()
        discoveryRetryJob?.cancel()
        pendingDirectReconnect = null
    }

    fun connectDevice(
        device: BluetoothDevice,
        sensorType: SensorType = SensorType.UNKNOWN,
    ) {
        connectDeviceInternal(
            device = device,
            sensorType = sensorType,
            clearPendingReconnect = true,
        )
    }

    private fun connectDeviceInternal(
        device: BluetoothDevice,
        sensorType: SensorType = SensorType.UNKNOWN,
        clearPendingReconnect: Boolean,
    ) {
        if (clearPendingReconnect) {
            stopScan()
        } else {
            bleScanner.stopScan()
            discoveryRetryJob?.cancel()
        }
        isConnecting = true
        currentDevice = device
        _currentDeviceAddress.value = device.address.uppercase()
        _currentDeviceName.value = device.name
        currentSensorType = resolveSensorType(device, sensorType)
        _connectionState.value = ConnectionState.CONNECTING
        bleConnector.connect(context, device, currentSensorType)
    }

    fun disconnect() {
        isConnecting = false
        directReconnectJob?.cancel()
        discoveryRetryJob?.cancel()
        pendingDirectReconnect = null
        bleConnector.disconnect()
    }

    private fun tryDirectReconnect(
        address: String,
        sensorType: SensorType,
    ): Boolean {
        val device = bleScanner.getRemoteDevice(address) ?: return false
        val resolvedSensorType = resolveSensorType(device, sensorType)
        SensorLogger.d(TAG, "Attempting direct reconnect to $address (type=$resolvedSensorType)")
        pendingDirectReconnect = DirectReconnectRequest(address.uppercase(), resolvedSensorType)
        connectDeviceInternal(
            device = device,
            sensorType = resolvedSensorType,
            clearPendingReconnect = false,
        )
        scheduleDirectReconnectFallback()
        return true
    }

    private fun scheduleDirectReconnectFallback() {
        directReconnectJob?.cancel()
        directReconnectJob =
            scope.launch {
                delay(DIRECT_RECONNECT_TIMEOUT_MS)
                if (pendingDirectReconnect != null &&
                    _connectionState.value != ConnectionState.CONNECTED
                ) {
                    fallbackToDiscoveryScan("direct reconnect timed out")
                }
            }
    }

    private fun fallbackToDiscoveryScan(reason: String) {
        val reconnectRequest = pendingDirectReconnect ?: return
        SensorLogger.w(TAG, "Falling back to BLE scan for ${reconnectRequest.address}: $reason")
        directReconnectJob?.cancel()
        pendingDirectReconnect = null
        bleConnector.disconnect()
        startDiscoveryScan(
            preferredAddress = reconnectRequest.address,
            preferredSensorType = reconnectRequest.sensorType,
        )
    }

    private fun startDiscoveryScan(
        preferredAddress: String?,
        preferredSensorType: SensorType,
        retryCount: Int = 0,
    ) {
        isConnecting = false
        _connectionState.value = ConnectionState.DISCONNECTED
        SensorLogger.d(TAG, "Starting BLE discovery scan (retry=$retryCount)")
        bleScanner.startScan(
            preferredTargetAddress = preferredAddress,
            onDeviceFound = { device ->
                val currentDevices = _devices.value.toMutableList()
                if (!currentDevices.contains(device)) {
                    currentDevices.add(device)
                    _devices.value = currentDevices
                    SensorLogger.d(TAG, "Device found: ${device.name ?: "Unknown"} (${device.address})")
                }
            },
            onTargetFound = { device ->
                if (isConnecting) {
                    SensorLogger.d(TAG, "Ignoring duplicate target while connecting: ${device.address}")
                    return@startScan
                }
                isConnecting = true
                val sensorType = resolveSensorType(device, preferredSensorType)
                SensorLogger.d(TAG, "Target sensor found: $sensorType (${device.address})")
                connectDevice(device, sensorType)
            },
            onScanFailed = { errorCode ->
                if (retryCount < MAX_SCAN_RECOVERY_RETRIES) {
                    SensorLogger.w(TAG, "Recovering BLE scan after error $errorCode")
                    discoveryRetryJob?.cancel()
                    discoveryRetryJob =
                        scope.launch {
                            delay(SCAN_RECOVERY_DELAY_MS)
                            startDiscoveryScan(
                                preferredAddress = preferredAddress,
                                preferredSensorType = preferredSensorType,
                                retryCount = retryCount + 1,
                            )
                        }
                } else {
                    SensorLogger.e(TAG, "BLE scan recovery exhausted after error $errorCode")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            },
        )
    }

    private fun resolveSensorType(
        device: BluetoothDevice,
        fallbackSensorType: SensorType,
    ): SensorType =
        SensorRegistry.getSensorType(device.address).takeIf { it != SensorType.UNKNOWN } ?: fallbackSensorType

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
    }

    private data class DirectReconnectRequest(
        val address: String,
        val sensorType: SensorType,
    )

    private companion object {
        const val TAG = "BleManager"
        const val DIRECT_RECONNECT_TIMEOUT_MS = 8_000L
        const val SCAN_RECOVERY_DELAY_MS = 700L
        const val MAX_SCAN_RECOVERY_RETRIES = 2
    }
}
