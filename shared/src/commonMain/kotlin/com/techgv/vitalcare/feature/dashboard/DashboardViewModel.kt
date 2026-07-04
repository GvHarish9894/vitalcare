package com.techgv.vitalcare.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.usecase.GetTodaySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

data class DashboardUiState(
    val date: LocalDate,
    val isLoading: Boolean = true,
    val count: Int = 0,
    val latest: VitalRecord? = null,
    val times: List<LocalTime> = emptyList(),
)

class DashboardViewModel(
    getTodaySummary: GetTodaySummary,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    private val today = clock.todayLocal(timeZone)

    val uiState: StateFlow<DashboardUiState> = getTodaySummary()
        .map { summary ->
            DashboardUiState(
                date = today,
                isLoading = false,
                count = summary.count,
                latest = summary.latest,
                times = summary.times,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(date = today),
        )
}
