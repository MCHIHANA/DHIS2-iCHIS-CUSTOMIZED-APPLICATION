package org.dhis2.sensors.devices.spo2

import java.util.UUID

object Spo2Constants {
    val serviceUuid: UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
    val measurementUuid: UUID = UUID.fromString("00001524-1212-efde-1523-785feabcd123")
    val cccdUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    val triggerCommand: ByteArray = byteArrayOf(0x01)
}
