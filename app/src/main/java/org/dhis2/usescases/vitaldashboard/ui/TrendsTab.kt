package org.dhis2.usescases.vitaldashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.dhis2.usescases.vitaldashboard.VitalDashboardData
import org.dhis2.usescases.vitaldashboard.model.VitalSignType

/**
 * Trends Tab Content
 * 
 * Displays trend charts for vital signs using MPAndroidChart
 * 
 * @author Shadreck Mkandawire
 */
@Composable
fun TrendsTabContent(
    data: VitalDashboardData,
    onVitalSignClick: (VitalSignType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(data.trends.entries.toList()) { (vitalType, trendData) ->
            VitalTrendCard(
                vitalType = vitalType,
                trendData = trendData,
                onClick = { onVitalSignClick(vitalType) }
            )
        }
    }
}

@Composable
fun VitalTrendCard(
    vitalType: VitalSignType,
    trendData: List<org.dhis2.usescases.vitaldashboard.TrendDataPoint>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = vitalType.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (trendData.isNotEmpty()) {
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            setTouchEnabled(true)
                            setDrawGridBackground(false)
                            setPinchZoom(true)
                            
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            axisRight.isEnabled = false
                            
                            legend.isEnabled = true
                        }
                    },
                    update = { chart ->
                        val entries = trendData.mapIndexed { index, point ->
                            Entry(index.toFloat(), point.value)
                        }
                        
                        val dataSet = LineDataSet(entries, vitalType.displayName).apply {
                            color = android.graphics.Color.BLUE
                            setCircleColor(android.graphics.Color.BLUE)
                            lineWidth = 2f
                            circleRadius = 4f
                            setDrawValues(false)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                        }
                        
                        chart.data = LineData(dataSet)
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${trendData.size} measurements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val abnormalCount = trendData.count { it.isAbnormal }
                    if (abnormalCount > 0) {
                        Text(
                            text = "$abnormalCount abnormal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    text = "No trend data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
