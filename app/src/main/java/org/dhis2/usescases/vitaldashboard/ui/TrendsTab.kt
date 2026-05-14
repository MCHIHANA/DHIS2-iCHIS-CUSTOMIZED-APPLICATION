package org.dhis2.usescases.vitaldashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dhis2.usescases.vitaldashboard.TrendSeries
import org.dhis2.usescases.vitaldashboard.VitalDashboardData
import org.dhis2.usescases.vitaldashboard.model.VitalSignType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Trends Tab — lightweight, no external chart library.
 *
 * Shows a simple text-based summary of recent readings per vital sign type:
 * latest value, min/max over the loaded window, and an abnormal count badge.
 * A small inline bar-indicator gives a quick visual sense of the value
 * relative to the normal range — all drawn with plain Compose.
 */
@Composable
fun TrendsTabContent(
    data: VitalDashboardData,
    onVitalSignClick: (VitalSignType) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (data.trends.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No trend data available yet.\nCapture more readings to see summaries here.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(data.trends.entries.toList()) { (vitalType, trendSeries) ->
            VitalTrendSummaryCard(
                vitalType = vitalType,
                trendSeries = trendSeries,
            )
        }
    }
}

@Composable
fun VitalTrendSummaryCard(
    vitalType: VitalSignType,
    trendSeries: List<TrendSeries>,
    modifier: Modifier = Modifier,
) {
    val allPoints = trendSeries.flatMap { it.points }
    val totalReadings = allPoints.size
    val abnormalCount = allPoints.count { it.isAbnormal }
    val latestValue = allPoints.maxByOrNull { it.timestamp }?.value
    val minValue = allPoints.minOfOrNull { it.value }
    val maxValue = allPoints.maxOfOrNull { it.value }
    val latestTimestamp = allPoints.maxOfOrNull { it.timestamp }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header row: vital sign name + abnormal badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = vitalType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (abnormalCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = "$abnormalCount abnormal",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else if (totalReadings > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = "All normal",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (totalReadings == 0) {
                Text(
                    text = "No readings recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Divider()

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TrendStatItem(
                    label = "Latest",
                    value = latestValue?.let { "%.1f %s".format(it, vitalType.unit) } ?: "--",
                    highlight = true,
                )
                TrendStatItem(
                    label = "Min",
                    value = minValue?.let { "%.1f".format(it) } ?: "--",
                )
                TrendStatItem(
                    label = "Max",
                    value = maxValue?.let { "%.1f".format(it) } ?: "--",
                )
                TrendStatItem(
                    label = "Readings",
                    value = totalReadings.toString(),
                )
            }

            // Simple inline range bar (latest value vs normal range)
            latestValue?.let { value ->
                vitalType.normalRange?.let { (low, high) ->
                    SimpleRangeBar(
                        value = value,
                        normalLow = low,
                        normalHigh = high,
                        unit = vitalType.unit,
                    )
                }
            }

            // Last reading timestamp
            latestTimestamp?.let { ts ->
                Text(
                    text = "Last reading: ${formatTrendTime(ts)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Per-series breakdown (e.g. Systolic / Diastolic for BP)
            if (trendSeries.size > 1) {
                Divider()
                trendSeries.forEach { series ->
                    val seriesLatest = series.points.maxByOrNull { it.timestamp }
                    if (seriesLatest != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = series.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "%.1f %s".format(seriesLatest.value, vitalType.unit),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (seriesLatest.isAbnormal) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendStatItem(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A simple horizontal bar that shows where the current value sits relative
 * to the normal range. Drawn entirely with Compose — no external library.
 *
 * Layout:  [LOW label]  [====normal range====]  [HIGH label]
 *                              ^ marker
 */
@Composable
private fun SimpleRangeBar(
    value: Float,
    normalLow: Float,
    normalHigh: Float,
    unit: String,
) {
    // Extend the visual range 20 % beyond normal on each side
    val visualLow = normalLow * 0.8f
    val visualHigh = normalHigh * 1.2f
    val totalSpan = (visualHigh - visualLow).coerceAtLeast(1f)

    val normalStartFraction = ((normalLow - visualLow) / totalSpan).coerceIn(0f, 1f)
    val normalEndFraction = ((normalHigh - visualLow) / totalSpan).coerceIn(0f, 1f)
    val valueFraction = ((value - visualLow) / totalSpan).coerceIn(0f, 1f)

    val isNormal = value in normalLow..normalHigh
    val markerColor = if (isNormal) Color(0xFF2E7D32) else Color(0xFFC62828)
    val normalBarColor = Color(0xFF81C784)
    val trackColor = Color(0xFFE0E0E0)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Normal range: ${"%.0f".format(normalLow)}–${"%.0f".format(normalHigh)} $unit",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(trackColor),
        ) {
            // Normal range segment
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalEndFraction)
                    .padding(start = (normalStartFraction * 1f).dp) // approximate; fine for display
                    .height(12.dp)
                    .background(normalBarColor),
            )
        }

        // Value marker row
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(valueFraction)
                    .height(4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Spacer(modifier = Modifier.fillMaxWidth(valueFraction))
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 16.dp)
                        .background(markerColor),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Current: ${"%.1f".format(value)} $unit",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = markerColor,
            )
            Text(
                text = if (isNormal) "✓ Normal" else "⚠ Outside range",
                style = MaterialTheme.typography.labelSmall,
                color = markerColor,
            )
        }
    }
}

private fun formatTrendTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hr ago"
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
