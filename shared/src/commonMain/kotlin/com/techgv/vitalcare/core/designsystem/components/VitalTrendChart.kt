package com.techgv.vitalcare.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
data class ChartPoint(val x: Float, val y: Float)

@Immutable
data class ChartSeries(val points: List<ChartPoint>, val color: Color)

/**
 * Hand-rolled Canvas line chart (D-012): gridlines, per-series path + dots,
 * min/max y labels. One calm hue per series; BP passes two series.
 * Callers render an EmptyState instead when there is no data (FR-AN4);
 * a single point is drawn as a centered dot.
 */
@Composable
fun VitalTrendChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    startLabel: String? = null,
    endLabel: String? = null,
    xRange: ClosedFloatingPointRange<Float>? = null,
) {
    val gridColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val textMeasurer = rememberTextMeasurer()

    val allPoints = series.flatMap { it.points }
    val yMinRaw = allPoints.minOfOrNull { it.y } ?: 0f
    val yMaxRaw = allPoints.maxOfOrNull { it.y } ?: 1f
    val pad = ((yMaxRaw - yMinRaw) * 0.15f).coerceAtLeast(2f)
    val yMin = yMinRaw - pad
    val yMax = yMaxRaw + pad
    val xMin = xRange?.start ?: allPoints.minOfOrNull { it.x } ?: 0f
    val xMax = xRange?.endInclusive ?: allPoints.maxOfOrNull { it.x } ?: 1f

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val labelGutter = 34.dp.toPx()
        val bottomGutter = if (startLabel != null || endLabel != null) 18.dp.toPx() else 4.dp.toPx()
        val chartWidth = size.width - labelGutter
        val chartHeight = size.height - bottomGutter

        fun toX(x: Float): Float =
            if (xMax == xMin) labelGutter + chartWidth / 2f
            else labelGutter + (x - xMin) / (xMax - xMin) * chartWidth

        fun toY(y: Float): Float =
            if (yMax == yMin) chartHeight / 2f
            else chartHeight - (y - yMin) / (yMax - yMin) * chartHeight

        // Gridlines + y labels at min / mid / max of the padded range.
        val gridValues = listOf(yMinRaw, (yMinRaw + yMaxRaw) / 2f, yMaxRaw)
        gridValues.forEach { value ->
            val gy = toY(value)
            drawLine(
                color = gridColor,
                start = Offset(labelGutter, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1.dp.toPx(),
            )
            drawGridLabel(textMeasurer, value.roundToInt().toString(), gy, labelStyle, labelColor)
        }

        series.forEach { s ->
            if (s.points.isEmpty()) return@forEach
            val sorted = s.points.sortedBy { it.x }
            if (sorted.size == 1) {
                drawCircle(color = s.color, radius = 5.dp.toPx(), center = Offset(toX(sorted[0].x), toY(sorted[0].y)))
            } else {
                val path = Path()
                sorted.forEachIndexed { i, p ->
                    if (i == 0) path.moveTo(toX(p.x), toY(p.y)) else path.lineTo(toX(p.x), toY(p.y))
                }
                drawPath(
                    path = path,
                    color = s.color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                sorted.forEach { p ->
                    drawCircle(color = s.color, radius = 3.5.dp.toPx(), center = Offset(toX(p.x), toY(p.y)))
                }
            }
        }

        startLabel?.let {
            val layout = textMeasurer.measure(it, labelStyle)
            drawText(layout, color = labelColor, topLeft = Offset(labelGutter, size.height - layout.size.height))
        }
        endLabel?.let {
            val layout = textMeasurer.measure(it, labelStyle)
            drawText(
                layout,
                color = labelColor,
                topLeft = Offset(size.width - layout.size.width, size.height - layout.size.height),
            )
        }
    }
}

private fun DrawScope.drawGridLabel(
    textMeasurer: TextMeasurer,
    text: String,
    y: Float,
    style: TextStyle,
    color: Color,
) {
    val layout = textMeasurer.measure(text, style)
    drawText(
        layout,
        color = color,
        topLeft = Offset(0f, (y - layout.size.height / 2f).coerceAtLeast(0f)),
    )
}
