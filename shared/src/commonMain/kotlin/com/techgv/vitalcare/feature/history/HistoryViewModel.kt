package com.techgv.vitalcare.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.usecase.ExportCsv
import com.techgv.vitalcare.domain.usecase.GetHistory
import com.techgv.vitalcare.domain.usecase.HistoryDaySection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.ALL,
    val query: String = "",
    val searchActive: Boolean = false,
    val sections: List<HistoryDaySection> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface HistoryEvent {
    data class FilterSelected(val filter: HistoryFilter) : HistoryEvent
    data class QueryChanged(val query: String) : HistoryEvent
    data object SearchToggled : HistoryEvent
    /** Export the currently filtered scope (FR-H6). */
    data object ExportClicked : HistoryEvent
}

sealed interface HistoryEffect {
    data object ExportEmpty : HistoryEffect
    data object ExportFailed : HistoryEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val getHistory: GetHistory,
    private val exportCsv: ExportCsv,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val query = MutableStateFlow("")
    private val searchActive = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> =
        combine(filter, query, searchActive) { filter, query, searchActive ->
            Triple(filter, query, searchActive)
        }.flatMapLatest { (filter, query, searchActive) ->
            getHistory(filter, query).map { sections ->
                HistoryUiState(
                    filter = filter,
                    query = query,
                    searchActive = searchActive,
                    sections = sections,
                    isLoading = false,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    private val _effects = Channel<HistoryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var exporting = false

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.FilterSelected -> filter.value = event.filter
            is HistoryEvent.QueryChanged -> query.value = event.query
            HistoryEvent.SearchToggled -> {
                searchActive.update { !it }
                if (!searchActive.value) query.value = ""
            }
            HistoryEvent.ExportClicked -> export()
        }
    }

    private fun export() {
        if (exporting) return
        exporting = true
        viewModelScope.launch {
            when (exportCsv(filter.value)) {
                is ExportCsv.Result.Exported -> Unit // share sheet is already up
                is ExportCsv.Result.Empty -> _effects.send(HistoryEffect.ExportEmpty)
                is ExportCsv.Result.Failed -> _effects.send(HistoryEffect.ExportFailed)
            }
            exporting = false
        }
    }
}
