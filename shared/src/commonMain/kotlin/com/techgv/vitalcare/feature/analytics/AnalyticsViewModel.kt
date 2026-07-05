package com.techgv.vitalcare.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.AnalyticsData
import com.techgv.vitalcare.domain.model.AnalyticsRange
import com.techgv.vitalcare.domain.usecase.GetAnalytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

data class AnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.DAILY,
    val today: LocalDate,
    val data: AnalyticsData? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    getAnalytics: GetAnalytics,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    private val today = clock.todayLocal(timeZone)
    private val range = MutableStateFlow(AnalyticsRange.DAILY)

    val uiState: StateFlow<AnalyticsUiState> = range
        .flatMapLatest { selectedRange ->
            getAnalytics(selectedRange).map { data ->
                AnalyticsUiState(
                    range = selectedRange,
                    today = today,
                    data = data,
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsUiState(today = today),
        )

    fun onRangeSelected(selected: AnalyticsRange) {
        range.value = selected
    }
}
