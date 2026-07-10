package com.techgv.vitalcare.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.FluidDayBalance
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.model.VolumeUnit
import com.techgv.vitalcare.domain.repository.SettingsRepository
import com.techgv.vitalcare.domain.usecase.GetFluidBalanceToday
import com.techgv.vitalcare.domain.usecase.GetTodaySummary
import com.techgv.vitalcare.domain.usecase.ObserveBackupStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** FR-D3: shown only while Drive is connected — absent means no hint at all. */
sealed interface BackupHint {
    /** There are changes newer than the last backup (or none was ever made). */
    data object Pending : BackupHint
    data class BackedUp(val daysAgo: Int) : BackupHint
}

data class DashboardUiState(
    val date: LocalDate,
    val isLoading: Boolean = true,
    val count: Int = 0,
    val latest: VitalRecord? = null,
    val times: List<LocalTime> = emptyList(),
    val backupHint: BackupHint? = null,
    val fluidBalance: FluidDayBalance? = null,
    val volumeUnit: VolumeUnit = VolumeUnit.ML,
)

class DashboardViewModel(
    getTodaySummary: GetTodaySummary,
    observeBackupStatus: ObserveBackupStatus,
    getFluidBalanceToday: GetFluidBalanceToday,
    settingsRepository: SettingsRepository,
    clock: Clock,
    private val timeZone: TimeZone,
) : ViewModel() {

    private val today = clock.todayLocal(timeZone)

    val uiState: StateFlow<DashboardUiState> = combine(
        getTodaySummary(),
        observeBackupStatus(),
        getFluidBalanceToday(),
        settingsRepository.volumeUnit,
    ) { summary, backup, fluid, unit ->
        DashboardUiState(
            date = today,
            isLoading = false,
            count = summary.count,
            latest = summary.latest,
            times = summary.times,
            backupHint = when {
                !backup.connected -> null // quiet unless Drive is set up (03 §1)
                backup.unbackedCount > 0 || backup.lastBackupAt == 0L -> BackupHint.Pending
                else -> BackupHint.BackedUp(daysAgo = daysSince(backup.lastBackupAt))
            },
            fluidBalance = fluid,
            volumeUnit = unit,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(date = today),
    )

    private fun daysSince(epochMillis: Long): Int =
        Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(timeZone).date
            .daysUntil(today)
            .coerceAtLeast(0)
}
