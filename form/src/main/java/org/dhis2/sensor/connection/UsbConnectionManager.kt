package org.dhis2.sensor.connection

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

class UsbConnectionManager(
    private val context: Context
) {

    private var onDeviceFound: ((UsbDevice) -> Unit)? = null
    private var onNoDeviceFound: (() -> Unit)? = null

    fun setCallbacks(
        onDeviceFound: (UsbDevice) -> Unit,
        onNoDeviceFound: () -> Unit
    ) {
        this.onDeviceFound = onDeviceFound
        this.onNoDeviceFound = onNoDeviceFound
    }

    fun connectUsb() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList

        if (deviceList.isEmpty()) {
            showUsbNotFound()
        } else {
            connectFirstUsb(deviceList)
        }
    }

    fun scanForDevices(): List<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.values.toList()
    }

    private fun showUsbNotFound() {
        Log.d("UsbConnectionManager", "No USB devices found")
        onNoDeviceFound?.invoke()
    }

    private fun connectFirstUsb(deviceList: HashMap<String, UsbDevice>) {
        val device = deviceList.values.firstOrNull()
        if (device != null) {
            Log.d("UsbConnectionManager", "Connecting to USB device: ${device.deviceName}")
            onDeviceFound?.invoke(device)
        } else {
            onNoDeviceFound?.invoke()
        }
    }
}
