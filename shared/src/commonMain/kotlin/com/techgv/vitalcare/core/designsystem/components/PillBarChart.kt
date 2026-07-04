package com.techgv.vitalcare.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Rounded-pill bar chart (03 §4.5). One value per slot; null = no data for
 * that slot (an empty track is shown so the axis stays readable).
 */
@Composable
fun PillBarChart(
    values: List<Float?>,
    barColor: Color,
    modifier: Modifier = Modifier,
    labels: List<String>? = null,
) {
    val max = values.filterNotNull().maxOrNull()?.takeIf { it > 0f } ?: 1f
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEach { value ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 22.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(trackColor, CircleShape),
                    )
                    if (value != null) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 22.dp)
                                .fillMaxWidth()
                                .fillMaxHeight((value / max).coerceIn(0.06f, 1f))
                                .background(barColor, CircleShape),
                        )
                    }
                }
            }
        }
        if (labels != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
