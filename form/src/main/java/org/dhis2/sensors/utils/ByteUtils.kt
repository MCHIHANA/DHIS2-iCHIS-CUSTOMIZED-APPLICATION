package org.dhis2.sensors.utils

object ByteUtils {
    fun formatBytes(data: ByteArray): String = data.joinToString(" ") { "%02X".format(it) }
}
