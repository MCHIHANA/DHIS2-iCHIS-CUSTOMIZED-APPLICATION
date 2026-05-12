package org.dhis2.usescases.vitaldashboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dhis2.usescases.vitaldashboard.PatientVitalSummary
import org.dhis2.usescases.vitaldashboard.VitalDashboardData
import org.dhis2.usescases.vitaldashboard.model.VitalSignType
import org.dhis2.usescases.vitaldashboard.model.VitalStatusColor
import java.text.SimpleDateFormat
import java.util.*

/**
 * Overview Tab Content
 * 
 * Displays patient summary cards with latest vital signs
 * 
 * @author Shadreck Mkandawire
 */
@Composable
fun OverviewTabContent(
    data: VitalDashboardData,
    onPatientClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Statistics Summary
        item {
            StatisticsSummaryCard(statistics = data.statistics)
        }

        // Patient Cards
        items(data.patientSummaries) { patient ->
            PatientVitalCard(
                patient = patient,
                onClick = { onPatientClick(patient.patientUid) }
            )
        }
    }
}

@Composable
fun StatisticsSummaryCard(
    statistics: org.dhis2.usescases.vitaldashboard.VitalStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dashboard Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Patients",
                    value = statistics.totalPatients.toString()
                )
                StatisticItem(
                    label = "Measurements",
                    value = statistics.totalMeasurements.toString()
                )
                StatisticItem(
                    label = "Active Alerts",
                    value = statistics.activeAlerts.toString(),
                    valueColor = if (statistics.activeAlerts > 0) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatisticItem(
                    label = "Today",
                    value = statistics.measurementsToday.toString()
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun PatientVitalCard(
    patient: PatientVitalSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Patient Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = patient.patientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        patient.age?.let { age ->
                            Text(
                                text = "$age years",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        patient.gender?.let { gender ->
                            Text(
                                text = gender,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (patient.hasAlerts) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "⚠ Alert",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Divider()

            // Vital Signs Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                patient.latestVitals.entries.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (vitalType, value) ->
                            VitalSignItem(
                                vitalType = vitalType,
                                value = value.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Last Measurement Time
            Text(
                text = "Last updated: ${formatTimestamp(patient.lastMeasurementTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VitalSignItem(
    vitalType: VitalSignType,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = vitalType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
