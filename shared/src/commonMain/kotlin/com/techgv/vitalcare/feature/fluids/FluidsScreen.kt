package com.techgv.vitalcare.feature.fluids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techgv.vitalcare.core.designsystem.components.BentoTile
import com.techgv.vitalcare.core.designsystem.components.CircleIconButton
import com.techgv.vitalcare.core.designsystem.components.ConfirmDialog
import com.techgv.vitalcare.core.designsystem.components.PillBarChart
import com.techgv.vitalcare.core.designsystem.components.SecondaryButton
import com.techgv.vitalcare.core.designsystem.components.SectionHeader
import com.techgv.vitalcare.core.designsystem.components.VitalValueDisplay
import com.techgv.vitalcare.core.designsystem.theme.VitalCareTheme
import com.techgv.vitalcare.core.util.TimeFormat
import com.techgv.vitalcare.domain.model.AnalyticsRange
import com.techgv.vitalcare.domain.model.FluidAnalyticsData
import com.techgv.vitalcare.domain.model.FluidDayBalance
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.SeriesPoint
import com.techgv.vitalcare.domain.model.VolumeUnit
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.action_back
import vitalcare.shared.generated.resources.action_cancel
import vitalcare.shared.generated.resources.action_delete
import vitalcare.shared.generated.resources.dashboard_fluid_log
import vitalcare.shared.generated.resources.dashboard_fluid_title
import vitalcare.shared.generated.resources.error_generic
import vitalcare.shared.generated.resources.export_scope_title
import vitalcare.shared.generated.resources.filter_all
import vitalcare.shared.generated.resources.filter_month
import vitalcare.shared.generated.resources.filter_today
import vitalcare.shared.generated.resources.filter_week
import vitalcare.shared.generated.resources.fluid_delete_message
import vitalcare.shared.generated.resources.fluid_delete_title
import vitalcare.shared.generated.resources.fluid_export_empty
import vitalcare.shared.generated.resources.fluid_goal
import vitalcare.shared.generated.resources.fluid_goal_progress
import vitalcare.shared.generated.resources.fluid_intake
import vitalcare.shared.generated.resources.fluid_net
import vitalcare.shared.generated.resources.fluid_output
import vitalcare.shared.generated.resources.fluids_add_custom
import vitalcare.shared.generated.resources.fluids_add_intake
import vitalcare.shared.generated.resources.fluids_add_urine
import vitalcare.shared.generated.resources.fluids_empty_message
import vitalcare.shared.generated.resources.fluids_entries_title
import vitalcare.shared.generated.resources.fluids_export_csv
import vitalcare.shared.generated.resources.fluids_range_total_intake
import vitalcare.shared.generated.resources.fluids_range_total_output
import vitalcare.shared.generated.resources.fluids_title
import vitalcare.shared.generated.resources.fluids_trend
import vitalcare.shared.generated.resources.range_daily
import vitalcare.shared.generated.resources.range_monthly
import vitalcare.shared.generated.resources.range_weekly
import vitalcare.shared.generated.resources.snackbar_fluid_deleted
import vitalcare.shared.generated.resources.snackbar_intake_logged
import vitalcare.shared.generated.resources.snackbar_urine_logged

@Composable
fun FluidsScreen(
    onNavigateBack: () -> Unit,
    onEditEntry: (String) -> Unit,
    onAddCustom: (FluidType) -> Unit,
    showSnackbar: (String) -> Unit,
) {
    val viewModel = koinViewModel<FluidsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val intakeLogged = stringResource(Res.string.snackbar_intake_logged)
    val urineLogged = stringResource(Res.string.snackbar_urine_logged)
    val deletedMessage = stringResource(Res.string.snackbar_fluid_deleted)
    val emptyMessage = stringResource(Res.string.fluid_export_empty)
    val genericError = stringResource(Res.string.error_generic)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FluidsEffect.Logged -> showSnackbar(
                    when (effect.type) {
                        FluidType.INTAKE -> intakeLogged
                        FluidType.OUTPUT -> urineLogged
                    },
                )
                FluidsEffect.Deleted -> showSnackbar(deletedMessage)
                FluidsEffect.SaveFailed -> showSnackbar(genericError)
                FluidsEffect.ExportEmpty -> showSnackbar(emptyMessage)
                FluidsEffect.ExportFailed -> showSnackbar(genericError)
            }
        }
    }

    FluidsContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onEditEntry = onEditEntry,
        onAddCustom = onAddCustom,
    )

    if (uiState.showExportChooser) {
        ExportScopeDialog(
            onSelect = { viewModel.onEvent(FluidsEvent.ExportScopeSelected(it)) },
            onDismiss = { viewModel.onEvent(FluidsEvent.ExportDismissed) },
        )
    }
    uiState.deleteTargetId?.let {
        ConfirmDialog(
            title = stringResource(Res.string.fluid_delete_title),
            message = stringResource(Res.string.fluid_delete_message),
            confirmLabel = stringResource(Res.string.action_delete),
            dismissLabel = stringResource(Res.string.action_cancel),
            onConfirm = { viewModel.onEvent(FluidsEvent.DeleteConfirmed) },
            onDismiss = { viewModel.onEvent(FluidsEvent.DeleteDismissed) },
            destructive = true,
        )
    }
}

@Composable
private fun FluidsContent(
    uiState: FluidsUiState,
    onEvent: (FluidsEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onEditEntry: (String) -> Unit,
    onAddCustom: (FluidType) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(Res.string.action_back),
                onClick = onNavigateBack,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(Res.string.fluids_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            CircleIconButton(
                icon = Icons.Rounded.Download,
                contentDescription = stringResource(Res.string.fluids_export_csv),
                onClick = { onEvent(FluidsEvent.ExportClicked) },
                enabled = !uiState.isExporting,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BalanceCard(
                balance = uiState.balance,
                unit = uiState.unit,
                onLog = { onAddCustom(FluidType.INTAKE) },
            )
            // One-tap logging, no mode toggle (03 §3.11): each type has its own panel.
            QuickLogPanel(
                title = stringResource(Res.string.fluids_add_intake),
                icon = Icons.Rounded.LocalDrink,
                tint = VitalCareTheme.colors.tintBlue,
                unit = uiState.unit,
                onAdd = { ml -> onEvent(FluidsEvent.QuickAdd(FluidType.INTAKE, ml)) },
                onCustom = { onAddCustom(FluidType.INTAKE) },
            )
            QuickLogPanel(
                title = stringResource(Res.string.fluids_add_urine),
                icon = Icons.Rounded.Wc,
                tint = VitalCareTheme.colors.tintPeach,
                unit = uiState.unit,
                onAdd = { ml -> onEvent(FluidsEvent.QuickAdd(FluidType.OUTPUT, ml)) },
                onCustom = { onAddCustom(FluidType.OUTPUT) },
            )
            uiState.balance?.entries?.takeIf { it.isNotEmpty() }?.let { entries ->
                EntriesSection(
                    entries = entries,
                    unit = uiState.unit,
                    onEdit = onEditEntry,
                    onDelete = { onEvent(FluidsEvent.DeleteRequested(it)) },
                )
            }
            TrendSection(
                range = uiState.range,
                analytics = uiState.analytics,
                unit = uiState.unit,
                onRangeChange = { onEvent(FluidsEvent.RangeChanged(it)) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BalanceCard(
    balance: FluidDayBalance?,
    unit: VolumeUnit,
    onLog: () -> Unit,
) {
    BentoTile(
        tint = VitalCareTheme.colors.tintBlue,
        hero = true,
        icon = Icons.Rounded.WaterDrop,
    ) {
        Text(
            text = stringResource(Res.string.dashboard_fluid_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(14.dp))
        if (balance == null || balance.isEmpty) {
            Text(
                text = stringResource(Res.string.fluids_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(Res.string.dashboard_fluid_log),
                onClick = onLog,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VitalValueDisplay(
                    value = unit.format(balance.intakeMl),
                    unit = unit.label(),
                    label = stringResource(Res.string.fluid_intake),
                    modifier = Modifier.weight(1f),
                )
                VitalValueDisplay(
                    value = unit.format(balance.outputMl),
                    unit = unit.label(),
                    label = stringResource(Res.string.fluid_output),
                    modifier = Modifier.weight(1f),
                )
                VitalValueDisplay(
                    value = signedAmount(balance.netMl, unit),
                    unit = unit.label(),
                    label = stringResource(Res.string.fluid_net),
                    modifier = Modifier.weight(1f),
                )
            }
            if (balance.goalMl > 0) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { balance.goalProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        Res.string.fluid_goal_progress,
                        unit.format(balance.intakeMl),
                        unit.format(balance.goalMl),
                        unit.label(),
                        "${(balance.goalProgress * 100).roundToInt()}%",
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * One-tap quick-log panel (03 §3.11): the type is fixed by the panel, so a
 * preset tap logs immediately — no mode toggle to check first.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLogPanel(
    title: String,
    icon: ImageVector,
    tint: Color,
    unit: VolumeUnit,
    onAdd: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = tint,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FluidPresetsMl.forEach { ml ->
                    FilterChip(
                        selected = false,
                        onClick = { onAdd(ml) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        label = { Text(formatAmount(ml, unit)) },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = onCustom,
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    label = { Text(stringResource(Res.string.fluids_add_custom)) },
                )
            }
        }
    }
}

@Composable
private fun EntriesSection(
    entries: List<FluidEntry>,
    unit: VolumeUnit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    SectionHeader(stringResource(Res.string.fluids_entries_title))
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            entries.forEach { entry ->
                Surface(
                    onClick = { onEdit(entry.id) },
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Type-coded icon chip (03 §3.11): blue drink = intake, peach WC = urine.
                        Surface(
                            shape = CircleShape,
                            color = when (entry.type) {
                                FluidType.INTAKE -> VitalCareTheme.colors.tintBlue
                                FluidType.OUTPUT -> VitalCareTheme.colors.tintPeach
                            },
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (entry.type) {
                                        FluidType.INTAKE -> Icons.Rounded.LocalDrink
                                        FluidType.OUTPUT -> Icons.Rounded.Wc
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    when (entry.type) {
                                        FluidType.INTAKE -> Res.string.fluid_intake
                                        FluidType.OUTPUT -> Res.string.fluid_output
                                    },
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = entry.note
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { "${TimeFormat.format(entry.time)} · $it" }
                                    ?: TimeFormat.format(entry.time),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = formatAmount(entry.amountMl, unit),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { onDelete(entry.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(Res.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendSection(
    range: AnalyticsRange,
    analytics: FluidAnalyticsData?,
    unit: VolumeUnit,
    onRangeChange: (AnalyticsRange) -> Unit,
) {
    SectionHeader(stringResource(Res.string.fluids_trend))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalyticsRange.entries.forEach { r ->
            FilterChip(
                selected = r == range,
                onClick = { onRangeChange(r) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                label = {
                    Text(
                        text = stringResource(
                            when (r) {
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
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        VitalValueDisplay(
            value = unit.format(analytics?.totalIntakeMl ?: 0),
            unit = unit.label(),
            label = stringResource(Res.string.fluids_range_total_intake),
            modifier = Modifier.weight(1f),
        )
        VitalValueDisplay(
            value = unit.format(analytics?.totalOutputMl ?: 0),
            unit = unit.label(),
            label = stringResource(Res.string.fluids_range_total_output),
            modifier = Modifier.weight(1f),
        )
    }
    val slots = when (range) {
        AnalyticsRange.DAILY -> 0
        AnalyticsRange.WEEKLY -> 7
        AnalyticsRange.MONTHLY -> 30
    }
    if (analytics != null && slots > 0) {
        Spacer(Modifier.height(12.dp))
        PillBarChart(
            values = bars(analytics.intake, slots),
            barColor = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Map day-offset series points into fixed slots for the bar chart. */
private fun bars(points: List<SeriesPoint>, slots: Int): List<Float?> {
    val arr = arrayOfNulls<Float>(slots)
    points.forEach { p ->
        val i = p.x.roundToInt()
        if (i in 0 until slots) arr[i] = p.value
    }
    return arr.toList()
}

@Composable
private fun ExportScopeDialog(
    onSelect: (HistoryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(Res.string.export_scope_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                HistoryFilter.entries.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = false, onClick = { onSelect(filter) })
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = false, onClick = { onSelect(filter) })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                when (filter) {
                                    HistoryFilter.ALL -> Res.string.filter_all
                                    HistoryFilter.TODAY -> Res.string.filter_today
                                    HistoryFilter.WEEK -> Res.string.filter_week
                                    HistoryFilter.MONTH -> Res.string.filter_month
                                },
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
