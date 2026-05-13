package org.dhis2.usescases.vitaldashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dhis2.usescases.vitaldashboard.AlertType
import org.dhis2.usescases.vitaldashboard.VitalAlert
import java.text.SimpleDateFormat
import java.util.*

/**
 * Alerts Tab Content
 * 
 * Displays medical alerts for abnormal vital signs
 * 
 * @author Shadreck Mkandawire
 */
@Composable
fun AlertsTabContent(
    alerts: List<VitalAlert>,
    onPatientClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No Active Alerts",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "All vital signs are within normal ranges",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alerts) { alert ->
                AlertCard(
                    alert = alert,
                    onClick = { onPatientClick(alert.patientUid) }
                )
            }
        }
    }
}

@Composable
fun AlertCard(
    alert: VitalAlert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getAlertColor(alert.alertType)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Alert Icon
            Icon(
                imageVector = getAlertIcon(alert.alertType),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = getAlertIconColor(alert.alertType)
            )

            // Alert Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = alert.patientName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatAlertTime(alert.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AlertSummarySection(
    alerts: List<VitalAlert>,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    val criticalCount = alerts.count { 
        it.alertType == AlertType.CRITICAL_HIGH || it.alertType == AlertType.CRITICAL_LOW 
    }
    val warningCount = alerts.count { 
        it.alertType == AlertType.HIGH || it.alertType == AlertType.LOW 
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (criticalCount > 0) {
                AlertSummaryItem(
                    count = criticalCount,
                    label = "Critical",
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (warningCount > 0) {
                AlertSummaryItem(
                    count = warningCount,
                    label = "Warning",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            AlertSummaryItem(
                count = alerts.size,
                label = "Total Alerts",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun AlertSummaryItem(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun getAlertColor(alertType: AlertType): androidx.compose.ui.graphics.Color {
    return when (alertType) {
        AlertType.CRITICAL_HIGH, AlertType.CRITICAL_LOW -> 
            MaterialTheme.colorScheme.errorContainer
        AlertType.HIGH, AlertType.LOW -> 
            MaterialTheme.colorScheme.tertiaryContainer
        AlertType.ABNORMAL -> 
            MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
private fun getAlertIconColor(alertType: AlertType): androidx.compose.ui.graphics.Color {
    return when (alertType) {
        AlertType.CRITICAL_HIGH, AlertType.CRITICAL_LOW -> 
            MaterialTheme.colorScheme.error
        AlertType.HIGH, AlertType.LOW -> 
            MaterialTheme.colorScheme.tertiary
        AlertType.ABNORMAL -> 
            MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun getAlertIcon(alertType: AlertType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (alertType) {
        AlertType.CRITICAL_HIGH, AlertType.CRITICAL_LOW ->
            Icons.Outlined.Warning
        else ->
            Icons.Outlined.Info
    }
}

private fun formatAlertTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
