package org.dhis2.usescases.vitaldashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dhis2.usescases.vitaldashboard.RecentVitalReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentMeasurementsTabContent(
    measurements: List<RecentVitalReading>,
    onPatientClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (measurements.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No recent measurements",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(measurements) { reading ->
                RecentReadingCard(
                    reading = reading,
                    onClick = { onPatientClick(reading.patientUid) },
                )
            }
        }
    }
}

@Composable
fun RecentReadingCard(
    reading: RecentVitalReading,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (reading.hasAlerts) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = reading.patientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                reading.values.forEach { (vitalType, value) ->
                    Text(
                        text = "${vitalType.displayName}: $value",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = formatMeasurementTime(reading.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (reading.hasAlerts) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Text(
                        text = "Abnormal",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }
    }
}

private fun formatMeasurementTime(timestamp: Long): String {
    val date = Date(timestamp)
    val diff = System.currentTimeMillis() - timestamp

    return when {
        diff < 3_600_000 -> {
            val mins = diff / 60_000
            if (mins < 1) "Just now" else "$mins min ago  (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)})"
        }
        isSameDay(timestamp) ->
            "Today  ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
        isYesterday(timestamp) ->
            "Yesterday  ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
        else ->
            SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()).format(date)
    }
}

private fun isSameDay(timestamp: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
        cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun isYesterday(timestamp: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        add(java.util.Calendar.DAY_OF_YEAR, -1)
    }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
        cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}
