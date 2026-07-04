package com.techgv.vitalcare.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.usecase.GetHistory
import com.techgv.vitalcare.domain.usecase.HistoryDaySection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

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
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val getHistory: GetHistory,
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

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.FilterSelected -> filter.value = event.filter
            is HistoryEvent.QueryChanged -> query.value = event.query
            HistoryEvent.SearchToggled -> {
                searchActive.update { !it }
                if (!searchActive.value) query.value = ""
            }
        }
    }
}
