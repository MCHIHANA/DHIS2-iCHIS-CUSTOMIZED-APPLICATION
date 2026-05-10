package org.dhis2.sensors.oximeter.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dhis2.sensors.oximeter.data.ConnectionStatus
import org.dhis2.sensors.oximeter.data.DeviceInfo
import org.dhis2.sensors.oximeter.data.OximeterReading
import timber.log.Timber
import java.util.UUID

/**
 * Manages BLE connectivity and data reading for FORA O2 Pulse Oximeter.
 *
 * This class handles:
 * - BLE scanning for the FORA O2 device
 * - Connection and bonding
 * - Enabling notifications on the characteristic
 * - Parsing incoming data packets
 * - Automatic reconnection with exponential backoff
 *
 * @property context Application context
 */
class BleManager(private val context: Context) {

    companion object {
        // FORA O2 BLE UUIDs
        private val SERVICE_UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
        private val CHARACTERISTIC_UUID = UUID.fromString("00001524-1212-efde-1523-785feabcd123")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        
        // Device identifiers
        private const val DEVICE_NAME = "FORA O2"
        private const val DEVICE_MAC_ADDRESS = "C0:26:DA:17:D5:7D"
        
        // Reconnection settings
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val INITIAL_RECONNECT_DELAY = 2000L // 2 seconds
        private const val MAX_RECONNECT_DELAY = 8000L // 8 seconds
        
        // Scan timeout
        private const val SCAN_TIMEOUT_MS = 30000L // 30 seconds
    }

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var scanJob: Job? = null
    private var reconnectAttempts = 0
    private var reconnectDelay = INITIAL_RECONNECT_DELAY

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // State flows
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _latestReading = MutableStateFlow<OximeterReading?>(null)
    val latestReading: StateFlow<OximeterReading?> = _latestReading.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Checks if required BLE permissions are granted.
     */
    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Starts scanning for FORA O2 device.
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasRequiredPermissions()) {
            _errorMessage.value = "Bluetooth permissions not granted"
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _errorMessage.value = "Bluetooth is not enabled"
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }

        _connectionStatus.value = ConnectionStatus.SCANNING
        _errorMessage.value = null
        Timber.d("Starting BLE scan for FORA O2")

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothAdapter.bluetoothLeScanner?.startScan(
            listOf(scanFilter),
            scanSettings,
            scanCallback
        )

        // Stop scan after timeout
        scanJob?.cancel()
        scanJob = scope.launch {
            delay(SCAN_TIMEOUT_MS)
            stopScan()
            if (_connectionStatus.value == ConnectionStatus.SCANNING) {
                _errorMessage.value = "Device not found. Please ensure FORA O2 is powered on and nearby."
                _connectionStatus.value = ConnectionStatus.ERROR
            }
        }
    }

    /**
     * Stops BLE scanning.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!hasRequiredPermissions()) return
        
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanJob?.cancel()
        Timber.d("BLE scan stopped")
    }

    /**
     * Disconnects from the current device and cleans up resources.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        bluetoothGatt?.let { gatt ->
            if (hasRequiredPermissions()) {
                gatt.disconnect()
                gatt.close()
            }
        }
        bluetoothGatt = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        reconnectAttempts = 0
        reconnectDelay = INITIAL_RECONNECT_DELAY
        Timber.d("Disconnected from device")
    }

    /**
     * Cleans up resources. Call this when the manager is no longer needed.
     */
    fun cleanup() {
        disconnect()
        scope.launch { }.cancel()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            @SuppressLint("MissingPermission")
            if (hasRequiredPermissions()) {
                val deviceName = result.device.name
                val deviceAddress = result.device.address
                
                Timber.d("Found device: $deviceName ($deviceAddress)")
                
                if (deviceName == DEVICE_NAME || deviceAddress == DEVICE_MAC_ADDRESS) {
                    stopScan()
                    connectToDevice(result.device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _errorMessage.value = "Scan failed with error code: $errorCode"
            _connectionStatus.value = ConnectionStatus.ERROR
            Timber.e("BLE scan failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (!hasRequiredPermissions()) return

        _connectionStatus.value = ConnectionStatus.CONNECTING
        Timber.d("Connecting to ${device.address}")

        bluetoothGatt = device.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (!hasRequiredPermissions()) return

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("Connected to GATT server")
                    _connectionStatus.value = ConnectionStatus.BONDING
                    
                    // Check bond state
                    when (gatt.device.bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            // Already bonded, discover services
                            gatt.discoverServices()
                        }
                        BluetoothDevice.BOND_NONE -> {
                            // Need to bond
                            gatt.device.createBond()
                        }
                        BluetoothDevice.BOND_BONDING -> {
                            // Bonding in progress, wait for broadcast
                            Timber.d("Bonding in progress...")
                        }
                    }
                    
                    // Reset reconnection parameters on successful connection
                    reconnectAttempts = 0
                    reconnectDelay = INITIAL_RECONNECT_DELAY
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.d("Disconnected from GATT server")
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    
                    // Attempt reconnection with exponential backoff
                    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                        scope.launch {
                            reconnectAttempts++
                            Timber.d("Reconnection attempt $reconnectAttempts after ${reconnectDelay}ms")
                            delay(reconnectDelay)
                            reconnectDelay = (reconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
                            connectToDevice(gatt.device)
                        }
                    } else {
                        _errorMessage.value = "Connection lost. Please reconnect manually."
                        _connectionStatus.value = ConnectionStatus.ERROR
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (!hasRequiredPermissions()) return

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("Services discovered")
                
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        enableNotifications(gatt, characteristic)
                        
                        // Update device info
                        _deviceInfo.value = DeviceInfo(
                            macAddress = gatt.device.address
                        )
                    } else {
                        _errorMessage.value = "Characteristic not found"
                        _connectionStatus.value = ConnectionStatus.ERROR
                        Timber.e("Characteristic $CHARACTERISTIC_UUID not found")
                    }
                } else {
                    _errorMessage.value = "Service not found"
                    _connectionStatus.value = ConnectionStatus.ERROR
                    Timber.e("Service $SERVICE_UUID not found")
                }
            } else {
                _errorMessage.value = "Service discovery failed"
                _connectionStatus.value = ConnectionStatus.ERROR
                Timber.e("Service discovery failed with status: $status")
            }
        }

        @Suppress("DEPRECATION")
        @SuppressLint("MissingPermission")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (!hasRequiredPermissions()) return

            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                val data = characteristic.value
                parseOximeterData(data)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (!hasRequiredPermissions()) return

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.uuid == CCCD_UUID) {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    Timber.d("Notifications enabled successfully")
                }
            } else {
                _errorMessage.value = "Failed to enable notifications"
                _connectionStatus.value = ConnectionStatus.ERROR
                Timber.e("Failed to write descriptor: $status")
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (!hasRequiredPermissions()) return

        // Enable local notifications
        val success = gatt.setCharacteristicNotification(characteristic, true)
        if (!success) {
            _errorMessage.value = "Failed to enable local notifications"
            _connectionStatus.value = ConnectionStatus.ERROR
            Timber.e("Failed to enable local notifications")
            return
        }

        // Write to CCCD descriptor to enable remote notifications
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
            Timber.d("Enabling notifications on characteristic")
        } else {
            _errorMessage.value = "CCCD descriptor not found"
            _connectionStatus.value = ConnectionStatus.ERROR
            Timber.e("CCCD descriptor not found")
        }
    }

    /**
     * Parses raw byte data from the oximeter.
     *
     * Data format (based on typical pulse oximeter BLE protocol):
     * Byte 0: SpO2 percentage (0-100)
     * Byte 1: Pulse rate high byte
     * Byte 2: Pulse rate low byte
     *
     * TODO: Verify this format with actual device. May need adjustment based on
     * real data packets from FORA O2.
     */
    private fun parseOximeterData(data: ByteArray?) {
        if (data == null || data.size < 3) {
            Timber.w("Invalid data packet: ${data?.contentToString()}")
            return
        }

        try {
            val spo2 = data[0].toInt() and 0xFF
            val heartRateHigh = data[1].toInt() and 0xFF
            val heartRateLow = data[2].toInt() and 0xFF
            val heartRate = (heartRateHigh shl 8) or heartRateLow

            // Validate readings
            if (spo2 in 0..100 && heartRate in 30..250) {
                val reading = OximeterReading(
                    spo2 = spo2,
                    heartRateBpm = heartRate
                )
                _latestReading.value = reading
                Timber.d("Parsed reading: SpO2=$spo2%, HR=$heartRate BPM")
            } else {
                Timber.w("Invalid reading values: SpO2=$spo2, HR=$heartRate")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing oximeter data")
        }
    }
}
