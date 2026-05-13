package org.dhis2.form.ui.sensor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class InterpretationColor(val color: Color) {
    NORMAL(Color(0xFF4CAF50)), // Green
    WARNING(Color(0xFFFFC107)), // Yellow
    CRITICAL(Color(0xFFF44336)) // Red
}

data class MedicalInterpretation(
    val icon: ImageVector,
    val status: String,
    val rangeReference: String,
    val interpretationColor: InterpretationColor
)

object MedicalInterpretationLayer {

    fun interpret(uid: String, label: String, valueStr: String?): MedicalInterpretation? {
        if (valueStr.isNullOrEmpty()) return null
        val value = valueStr.toDoubleOrNull() ?: return null

        val isTemperature = uid == "KXNH45ts16S" || label.contains("temperature", ignoreCase = true)
        val isSystolic = uid == "HkfzcXMdLLF" || label.contains("systolic", ignoreCase = true)
        val isDiastolic = uid == "BaGxiB8AsNI" || label.contains("diastolic", ignoreCase = true)
        val isHeartRate = uid == "S7OjKl85YSh" || uid == "tZbUrUbhUNy" || label.contains("heart rate", ignoreCase = true) || label.contains("heartrate", ignoreCase = true) || label.contains("pulse", ignoreCase = true) || label.contains("bpm", ignoreCase = true)

        return when {
            isTemperature -> interpretTemperature(value)
            isSystolic -> interpretSystolic(value)
            isDiastolic -> interpretDiastolic(value)
            isHeartRate -> interpretHeartRate(value)
            else -> null
        }
    }

    private fun interpretTemperature(value: Double): MedicalInterpretation {
        return when {
            value < 36.0 -> MedicalInterpretation(Icons.Filled.ArrowDownward, "Hypothermia", "< 36.0°C", InterpretationColor.WARNING)
            value <= 37.5 -> MedicalInterpretation(Icons.Filled.CheckCircle, "Normothermia", "36.0°C - 37.5°C", InterpretationColor.NORMAL)
            else -> MedicalInterpretation(Icons.Filled.ArrowUpward, "Hyperthermia", "> 37.5°C", InterpretationColor.CRITICAL)
        }
    }

    private fun interpretSystolic(value: Double): MedicalInterpretation {
        return when {
            value < 90 -> MedicalInterpretation(Icons.Filled.ArrowDownward, "Hypotension", "< 90 mmHg", InterpretationColor.WARNING)
            value <= 120 -> MedicalInterpretation(Icons.Filled.CheckCircle, "Normal Systolic Pressure", "90 - 120 mmHg", InterpretationColor.NORMAL)
            else -> MedicalInterpretation(Icons.Filled.ArrowUpward, "Hypertension", "> 120 mmHg", InterpretationColor.CRITICAL)
        }
    }

    private fun interpretDiastolic(value: Double): MedicalInterpretation {
        return when {
            value < 60 -> MedicalInterpretation(Icons.Filled.ArrowDownward, "Low Diastolic Pressure", "< 60 mmHg", InterpretationColor.WARNING)
            value <= 80 -> MedicalInterpretation(Icons.Filled.CheckCircle, "Normal Diastolic Pressure", "60 - 80 mmHg", InterpretationColor.NORMAL)
            else -> MedicalInterpretation(Icons.Filled.ArrowUpward, "Elevated Diastolic Pressure", "> 80 mmHg", InterpretationColor.CRITICAL)
        }
    }

    private fun interpretHeartRate(value: Double): MedicalInterpretation {
        return when {
            value < 60 -> MedicalInterpretation(Icons.Filled.ArrowDownward, "Bradycardia", "< 60 BPM", InterpretationColor.WARNING)
            value <= 100 -> MedicalInterpretation(Icons.Filled.CheckCircle, "Normal Heart Rate", "60 - 100 BPM", InterpretationColor.NORMAL)
            else -> MedicalInterpretation(Icons.Filled.ArrowUpward, "Tachycardia", "> 100 BPM", InterpretationColor.CRITICAL)
        }
    }
}

@Composable
fun MedicalInterpretationUI(interpretation: MedicalInterpretation, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(interpretation.interpretationColor.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = interpretation.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = interpretation.status,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Range: ${interpretation.rangeReference}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 36.dp)
        )
    }
}
