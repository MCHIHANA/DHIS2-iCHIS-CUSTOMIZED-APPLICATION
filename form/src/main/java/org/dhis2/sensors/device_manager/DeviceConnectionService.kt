package org.dhis2.sensors.device_manager

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.dhis2.sensor.ble.BleManager
import org.dhis2.sensor.config.SensorConfigRepository
import org.dhis2.sensors.core.BleScanner

class DeviceConnectionService(
    context: Context,
    private val pairedDeviceRepository: PairedDeviceRepository,
    sensorConfigRepository: SensorConfigRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bleScanner = BleScanner(context)
    private val bleManager = BleManager(context, sensorConfigRepository)

    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availableDevices: StateFlow<List<BluetoothDevice>> = _availableDevices.asStateFlow()

    private val _pairingDeviceType = MutableStateFlow<DeviceType?>(null)
    val pairingDeviceType: StateFlow<DeviceType?> = _pairingDeviceType.asStateFlow()

    val connectionState = bleManager.connectionState
    val currentDeviceAddress = bleManager.currentDeviceAddress
    val currentDeviceName = bleManager.currentDeviceName
    val sensorData = bleManager.sensorData
    val lastFailure = bleManager.lastFailure

    private var requestedDeviceType: DeviceType? = null

    init {
        bleManager.connectionState
            .onEach { state ->
                if (state == BleManager.ConnectionState.CONNECTED) {
                    val address = bleManager.currentDeviceAddress.value ?: return@onEach
                    val deviceType =
                        requestedDeviceType
                            ?: pairedDeviceRepository.findDevice(address)?.deviceType
                            ?: return@onEach
                    pairedDeviceRepository.markDeviceConnected(
                        deviceName = bleManager.currentDeviceName.value ?: deviceType.defaultDeviceName,
                        macAddress = address,
                        deviceType = deviceType,
                    )
                }
            }.launchIn(scope)
    }

    fun startPairingScan(deviceType: DeviceType) {
        bleManager.disconnect()
        stopPairingScan()
        requestedDeviceType = null
        _pairingDeviceType.value = deviceType
        _availableDevices.value = emptyList()
        bleScanner.startScan(
            onDeviceFound = { device ->
                val currentDevices = _availableDevices.value
                if (currentDevices.none { it.address.equals(device.address, ignoreCase = true) }) {
                    _availableDevices.value = currentDevices + device
                }
            },
            onTargetFound = null,
        )
    }

    fun stopPairingScan() {
        bleScanner.stopScan()
        _pairingDeviceType.value = null
    }

    fun savePairedDevice(device: BluetoothDevice) {
        val deviceType = _pairingDeviceType.value ?: return
        pairedDeviceRepository.pairDevice(
            deviceName = device.name ?: deviceType.defaultDeviceName,
            macAddress = device.address,
            deviceType = deviceType,
        )
        stopPairingScan()
        _availableDevices.value = emptyList()
    }

    fun connectToDevice(device: SensorDevice) {
        stopPairingScan()
        requestedDeviceType = device.deviceType
        bleManager.startScan(
            preferredDeviceAddress = device.macAddress,
            sensorType = device.deviceType.toBleSensorType(),
        )
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun clear() {
        stopPairingScan()
        bleManager.stopScan()
        bleManager.disconnect()
        scope.cancel()
    }
}
