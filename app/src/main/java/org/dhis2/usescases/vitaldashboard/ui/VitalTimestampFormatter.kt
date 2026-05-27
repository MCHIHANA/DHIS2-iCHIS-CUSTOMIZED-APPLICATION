package org.dhis2.usescases.vitaldashboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val FRESHNESS_REFRESH_INTERVAL = 60_000L

@Composable
fun rememberDashboardClock(): Long {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(FRESHNESS_REFRESH_INTERVAL)
            value = System.currentTimeMillis()
        }
    }

    return now
}

fun formatDashboardTimestamp(timestamp: Long): String {
    val formatter =
        if (isSameDay(timestamp)) {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        } else {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        }

    return formatter.format(Date(timestamp))
}

fun formatReadingFreshness(
    timestamp: Long,
    now: Long,
): String {
    val diff = (now - timestamp).coerceAtLeast(0L)

    return when {
        diff < 60_000L -> "Fresh now"
        diff < 3_600_000L -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        else -> "${diff / 86_400_000L} day${if (diff >= 172_800_000L) "s" else ""} ago"
    }
}

private fun isSameDay(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }

    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}
