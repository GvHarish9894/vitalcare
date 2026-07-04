package com.techgv.vitalcare.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techgv.vitalcare.core.designsystem.components.CircleIconButton
import com.techgv.vitalcare.core.designsystem.components.EmptyState
import com.techgv.vitalcare.core.designsystem.components.VitalTextField
import com.techgv.vitalcare.core.util.DateLabel
import com.techgv.vitalcare.core.util.FullDateFormat
import com.techgv.vitalcare.core.util.TimeFormat
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.VitalRecord
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.action_search
import vitalcare.shared.generated.resources.filter_all
import vitalcare.shared.generated.resources.filter_month
import vitalcare.shared.generated.resources.filter_today
import vitalcare.shared.generated.resources.filter_week
import vitalcare.shared.generated.resources.header_today
import vitalcare.shared.generated.resources.header_yesterday
import vitalcare.shared.generated.resources.history_empty_action
import vitalcare.shared.generated.resources.history_empty_message
import vitalcare.shared.generated.resources.history_empty_title
import vitalcare.shared.generated.resources.history_no_matches
import vitalcare.shared.generated.resources.history_search_hint
import vitalcare.shared.generated.resources.history_title
import vitalcare.shared.generated.resources.unit_bpm
import vitalcare.shared.generated.resources.unit_percent

@Composable
fun HistoryScreen(
    onOpenDetails: (String) -> Unit,
    onRecordVitals: () -> Unit,
) {
    val viewModel = koinViewModel<HistoryViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onOpenDetails = onOpenDetails,
        onRecordVitals = onRecordVitals,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    onOpenDetails: (String) -> Unit,
    onRecordVitals: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.history_title),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.weight(1f),
            )
            CircleIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = stringResource(Res.string.action_search),
                onClick = { onEvent(HistoryEvent.SearchToggled) },
            )
        }
        Spacer(Modifier.padding(top = 6.dp))

        if (uiState.searchActive) {
            VitalTextField(
                value = uiState.query,
                onValueChange = { onEvent(HistoryEvent.QueryChanged(it)) },
                label = stringResource(Res.string.history_search_hint),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }

        FilterChips(selected = uiState.filter, onSelect = { onEvent(HistoryEvent.FilterSelected(it)) })

        val globallyEmpty = uiState.sections.isEmpty() &&
            uiState.filter == HistoryFilter.ALL && uiState.query.isBlank()
        when {
            uiState.isLoading -> Unit
            globallyEmpty -> EmptyState(
                icon = Icons.Rounded.History,
                title = stringResource(Res.string.history_empty_title),
                message = stringResource(Res.string.history_empty_message),
                actionLabel = stringResource(Res.string.history_empty_action),
                onAction = onRecordVitals,
                modifier = Modifier.padding(top = 40.dp),
            )
            uiState.sections.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Search,
                title = stringResource(Res.string.history_no_matches),
                modifier = Modifier.padding(top = 40.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.sections.forEach { section ->
                    stickyHeader(key = section.date.toString()) {
                        DateHeader(label = section.label)
                    }
                    items(
                        count = section.records.size,
                        key = { index -> section.records[index].id },
                    ) { index ->
                        RecordRow(record = section.records[index], onClick = onOpenDetails)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                label = {
                    Text(
                        text = stringResource(
                            when (filter) {
                                HistoryFilter.ALL -> Res.string.filter_all
                                HistoryFilter.TODAY -> Res.string.filter_today
                                HistoryFilter.WEEK -> Res.string.filter_week
                                HistoryFilter.MONTH -> Res.string.filter_month
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
private fun DateHeader(label: DateLabel) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = when (label) {
                DateLabel.Today -> stringResource(Res.string.header_today)
                DateLabel.Yesterday -> stringResource(Res.string.header_yesterday)
                is DateLabel.Other -> FullDateFormat.format(label.date)
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun RecordRow(record: VitalRecord, onClick: (String) -> Unit) {
    val percent = stringResource(Res.string.unit_percent)
    val bpm = stringResource(Res.string.unit_bpm)
    val summary = buildList {
        record.spo2?.let { add("$it $percent") }
        record.heartRate?.let { add("$it $bpm") }
        if (record.systolic != null && record.diastolic != null) {
            add("${record.systolic}/${record.diastolic}")
        }
    }.joinToString(" · ")

    Surface(
        onClick = { onClick(record.id) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = TimeFormat.format(record.time),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.padding(horizontal = 8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
