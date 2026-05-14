package org.dhis2.usescases.vitaldashboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dhis2.usescases.vitaldashboard.PatientVitalSummary
import org.dhis2.usescases.vitaldashboard.VitalDashboardData
import org.dhis2.usescases.vitaldashboard.VitalStatistics
import org.dhis2.usescases.vitaldashboard.model.VitalSignType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OverviewTabContent(
    data: VitalDashboardData,
    onPatientClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatisticsSummaryCard(statistics = data.statistics)
        }

        items(data.patientSummaries) { patient ->
            PatientVitalCard(
                patient = patient,
                onClick = { onPatientClick(patient.patientUid) },
            )
        }
    }
}

@Composable
fun StatisticsSummaryCard(
    statistics: VitalStatistics,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Dashboard Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatisticItem(
                    label = "Patients",
                    value = statistics.totalPatients.toString(),
                )
                StatisticItem(
                    label = "Measurements",
                    value = statistics.totalMeasurements.toString(),
                )
                StatisticItem(
                    label = "Active Alerts",
                    value = statistics.activeAlerts.toString(),
                    valueColor =
                        if (statistics.activeAlerts > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                )
                StatisticItem(
                    label = "Today",
                    value = statistics.measurementsToday.toString(),
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
fun PatientVitalCard(
    patient: PatientVitalSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = patient.patientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        patient.age?.let { age ->
                            Text(
                                text = "$age years",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        patient.gender?.let { gender ->
                            Text(
                                text = gender,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (patient.hasAlerts) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = "Alert",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            Divider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                patient.latestVitals.entries.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { (vitalType, value) ->
                            VitalSignItem(
                                vitalType = vitalType,
                                value = value.toString(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Text(
                text = "Last updated: ${formatTimestamp(patient.lastMeasurementTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun VitalSignItem(
    vitalType: VitalSignType,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = vitalType.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val date = Date(timestamp)

    return when {
        // Under 1 hour → show relative (useful for live clinical monitoring)
        diff < 3_600_000 -> {
            val mins = diff / 60_000
            if (mins < 1) "Just now" else "$mins min ago  (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)})"
        }
        // Same calendar day → show time only
        isSameDay(timestamp) ->
            "Today  ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
        // Yesterday
        isYesterday(timestamp) ->
            "Yesterday  ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
        // Older → full date + time
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
