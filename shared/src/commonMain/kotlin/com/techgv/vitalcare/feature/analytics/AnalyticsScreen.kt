package com.techgv.vitalcare.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techgv.vitalcare.core.designsystem.components.ChartPoint
import com.techgv.vitalcare.core.designsystem.components.ChartSeries
import com.techgv.vitalcare.core.designsystem.components.VitalTrendChart
import com.techgv.vitalcare.core.util.ShortDateFormat
import com.techgv.vitalcare.domain.model.AnalyticsRange
import com.techgv.vitalcare.domain.model.VitalSeries
import com.techgv.vitalcare.domain.model.VitalStats
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.analytics_empty
import vitalcare.shared.generated.resources.analytics_title
import vitalcare.shared.generated.resources.header_today
import vitalcare.shared.generated.resources.range_daily
import vitalcare.shared.generated.resources.range_monthly
import vitalcare.shared.generated.resources.range_weekly
import vitalcare.shared.generated.resources.stat_avg
import vitalcare.shared.generated.resources.stat_max
import vitalcare.shared.generated.resources.stat_min
import vitalcare.shared.generated.resources.vital_blood_pressure
import vitalcare.shared.generated.resources.vital_diastolic
import vitalcare.shared.generated.resources.vital_heart_rate
import vitalcare.shared.generated.resources.vital_spo2
import vitalcare.shared.generated.resources.vital_systolic

@Composable
fun AnalyticsScreen() {
    val viewModel = koinViewModel<AnalyticsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(Res.string.analytics_title),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 10.dp),
        )

        RangeChips(selected = uiState.range, onSelect = viewModel::onRangeSelected)

        val data = uiState.data
        if (data != null) {
            val xRange = chartDomain(uiState.range)
            val (startLabel, endLabel) = axisLabels(uiState.range, uiState.today)

            ChartCard(
                title = stringResource(Res.string.vital_spo2),
                series = listOf(data.spo2 to MaterialTheme.colorScheme.primary),
                statRows = listOf(null to data.spo2.stats),
                xRange = xRange,
                startLabel = startLabel,
                endLabel = endLabel,
            )
            ChartCard(
                title = stringResource(Res.string.vital_heart_rate),
                series = listOf(data.heartRate to MaterialTheme.colorScheme.tertiary),
                statRows = listOf(null to data.heartRate.stats),
                xRange = xRange,
                startLabel = startLabel,
                endLabel = endLabel,
            )
            ChartCard(
                title = stringResource(Res.string.vital_blood_pressure),
                series = listOf(
                    data.systolic to MaterialTheme.colorScheme.primary,
                    data.diastolic to MaterialTheme.colorScheme.tertiary,
                ),
                statRows = listOf(
                    stringResource(Res.string.vital_systolic) to data.systolic.stats,
                    stringResource(Res.string.vital_diastolic) to data.diastolic.stats,
                ),
                xRange = xRange,
                startLabel = startLabel,
                endLabel = endLabel,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun RangeChips(selected: AnalyticsRange, onSelect: (AnalyticsRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalyticsRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                label = {
                    Text(
                        text = stringResource(
                            when (range) {
                                AnalyticsRange.DAILY -> Res.string.range_daily
                                AnalyticsRange.WEEKLY -> Res.string.range_weekly
                                AnalyticsRange.MONTHLY -> Res.string.range_monthly
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    series: List<Pair<VitalSeries, Color>>,
    statRows: List<Pair<String?, VitalStats?>>,
    xRange: ClosedFloatingPointRange<Float>,
    startLabel: String,
    endLabel: String,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            val hasData = series.any { (vitalSeries, _) -> vitalSeries.points.isNotEmpty() }
            if (!hasData) {
                Text(
                    text = stringResource(Res.string.analytics_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 30.dp),
                )
            } else {
                VitalTrendChart(
                    series = series.map { (vitalSeries, color) ->
                        ChartSeries(
                            points = vitalSeries.points.map { ChartPoint(it.x, it.value) },
                            color = color,
                        )
                    },
                    xRange = xRange,
                    startLabel = startLabel,
                    endLabel = endLabel,
                )
                statRows.forEach { (label, stats) ->
                    if (stats != null) {
                        Spacer(Modifier.height(10.dp))
                        StatsRow(label = label, stats = stats)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(label: String?, stats: VitalStats) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        StatValue(stringResource(Res.string.stat_min), stats.min)
        StatValue(stringResource(Res.string.stat_avg), stats.avg)
        StatValue(stringResource(Res.string.stat_max), stats.max)
    }
}

@Composable
private fun StatValue(label: String, value: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value.toString(), style = MaterialTheme.typography.labelLarge)
    }
}

private fun chartDomain(range: AnalyticsRange): ClosedFloatingPointRange<Float> = when (range) {
    AnalyticsRange.DAILY -> 0f..(24f * 60f)
    AnalyticsRange.WEEKLY -> 0f..6f
    AnalyticsRange.MONTHLY -> 0f..29f
}

@Composable
private fun axisLabels(range: AnalyticsRange, today: LocalDate): Pair<String, String> =
    when (range) {
        AnalyticsRange.DAILY -> "00:00" to "24:00"
        AnalyticsRange.WEEKLY ->
            ShortDateFormat.format(today.minus(6, DateTimeUnit.DAY)) to
                stringResource(Res.string.header_today)
        AnalyticsRange.MONTHLY ->
            ShortDateFormat.format(today.minus(29, DateTimeUnit.DAY)) to
                stringResource(Res.string.header_today)
    }
