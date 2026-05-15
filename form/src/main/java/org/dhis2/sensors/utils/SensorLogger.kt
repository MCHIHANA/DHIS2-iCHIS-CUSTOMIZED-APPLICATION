package org.dhis2.sensors.utils

import android.util.Log

object SensorLogger {
    fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        Log.e(tag, message, throwable)
    }
}
