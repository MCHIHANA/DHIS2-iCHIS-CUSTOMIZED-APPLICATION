package org.dhis2.sensor.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages connection timeout for sensor scanning.
 * Automatically triggers timeout after 5 minutes if no device is found.
 */
object ConnectionTimeoutManager {

    private const val TIMEOUT_MS = 5 * 60 * 1000L   // 5 minutes

    private var timeoutJob: Job? = null

    /**
     * Starts the connection timeout.
     * If not cancelled within 5 minutes, onTimeout will be called.
     *
     * @param scope CoroutineScope to launch the timeout in
     * @param onTimeout Callback invoked when timeout occurs
     */
    fun startTimeout(
        scope: CoroutineScope,
        onTimeout: () -> Unit
    ) {
        // Cancel any existing timeout first
        cancelTimeout()

        timeoutJob = scope.launch {
            delay(TIMEOUT_MS)
            onTimeout()
        }
    }

    /**
     * Cancels the active timeout.
     * Call this when a device connects successfully.
     */
    fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    /**
     * Checks if a timeout is currently active.
     */
    fun isTimeoutActive(): Boolean {
        return timeoutJob?.isActive == true
    }
}
