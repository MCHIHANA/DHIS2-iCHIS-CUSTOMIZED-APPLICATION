package org.dhis2.usescases.vitaldashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dhis2.usescases.vitaldashboard.VitalMeasurement
import java.text.SimpleDateFormat
import java.util.*

/**
 * Recent Measurements Tab Content
 * 
 * Displays chronological list of recent vital sign measurements
 * 
 * @author Shadreck Mkandawire
 */
@Composable
fun RecentMeasurementsTabContent(
    measurements: List<VitalMeasurement>,
    onPatientClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (measurements.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recent measurements",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(measurements) { measurement ->
                MeasurementCard(
                    measurement = measurement,
                    onClick = { onPatientClick(measurement.patientUid) }
                )
            }
        }
    }
}

@Composable
fun MeasurementCard(
    measurement: VitalMeasurement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (measurement.isAbnormal) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = measurement.patientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = measurement.vitalSignType.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatMeasurementTime(measurement.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = measurement.value.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (measurement.isAbnormal) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                if (measurement.isAbnormal) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "Abnormal",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}

private fun formatMeasurementTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
