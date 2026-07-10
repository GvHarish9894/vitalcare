package com.techgv.vitalcare.feature.fluids

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.nowLocal
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.AnalyticsRange
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.VolumeUnit
import com.techgv.vitalcare.domain.repository.SettingsRepository
import com.techgv.vitalcare.domain.usecase.DeleteFluidEntry
import com.techgv.vitalcare.domain.usecase.ExportFluidCsv
import com.techgv.vitalcare.domain.usecase.GetFluidAnalytics
import com.techgv.vitalcare.domain.usecase.GetFluidBalanceToday
import com.techgv.vitalcare.domain.usecase.SaveFluidEntry
import com.techgv.vitalcare.domain.validation.FluidInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class FluidsViewModel(
    getFluidBalanceToday: GetFluidBalanceToday,
    private val getFluidAnalytics: GetFluidAnalytics,
    private val saveFluidEntry: SaveFluidEntry,
    private val deleteFluidEntry: DeleteFluidEntry,
    private val exportFluidCsv: ExportFluidCsv,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) : ViewModel() {

    private data class LocalUi(
        val range: AnalyticsRange = AnalyticsRange.WEEKLY,
        val isExporting: Boolean = false,
        val showExportChooser: Boolean = false,
        val deleteTargetId: String? = null,
    )

    private val localUi = MutableStateFlow(LocalUi())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val analyticsFlow = localUi
        .map { it.range }
        .distinctUntilChanged()
        .flatMapLatest { getFluidAnalytics(it) }

    val uiState: StateFlow<FluidsUiState> = combine(
        getFluidBalanceToday(),
        settingsRepository.volumeUnit,
        analyticsFlow,
        localUi,
    ) { balance, unit, analytics, local ->
        FluidsUiState(
            unit = unit,
            balance = balance,
            range = local.range,
            analytics = analytics,
            isExporting = local.isExporting,
            showExportChooser = local.showExportChooser,
            deleteTargetId = local.deleteTargetId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FluidsUiState(),
    )

    private val _effects = Channel<FluidsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: FluidsEvent) {
        when (event) {
            is FluidsEvent.QuickAdd -> quickAdd(event.type, event.amountMl)
            is FluidsEvent.RangeChanged -> localUi.update { it.copy(range = event.range) }
            is FluidsEvent.DeleteRequested ->
                localUi.update { it.copy(deleteTargetId = event.id) }
            FluidsEvent.DeleteConfirmed -> deleteConfirmed()
            FluidsEvent.DeleteDismissed -> localUi.update { it.copy(deleteTargetId = null) }
            FluidsEvent.ExportClicked -> localUi.update { it.copy(showExportChooser = true) }
            FluidsEvent.ExportDismissed -> localUi.update { it.copy(showExportChooser = false) }
            is FluidsEvent.ExportScopeSelected -> export(event.scope)
        }
    }

    private fun quickAdd(type: FluidType, amountMl: Int) {
        viewModelScope.launch {
            // Presets are canonical mL, so validate as mL regardless of display unit.
            val input = FluidInput(
                date = clock.todayLocal(timeZone),
                time = clock.nowLocal(timeZone).time.let { LocalTime(it.hour, it.minute) },
                type = type,
                amount = amountMl.toString(),
                unit = VolumeUnit.ML,
            )
            when (saveFluidEntry(input)) {
                is SaveFluidEntry.Result.Saved -> _effects.send(FluidsEffect.Logged(type))
                else -> _effects.send(FluidsEffect.SaveFailed)
            }
        }
    }

    private fun deleteConfirmed() {
        val id = localUi.value.deleteTargetId ?: return
        localUi.update { it.copy(deleteTargetId = null) }
        viewModelScope.launch {
            if (deleteFluidEntry(id) is AppResult.Success) {
                _effects.send(FluidsEffect.Deleted)
            }
        }
    }

    private fun export(scope: HistoryFilter) {
        if (localUi.value.isExporting) return
        localUi.update { it.copy(showExportChooser = false, isExporting = true) }
        viewModelScope.launch {
            when (exportFluidCsv(scope)) {
                is ExportFluidCsv.Result.Exported -> Unit // share sheet is already up
                is ExportFluidCsv.Result.Empty -> _effects.send(FluidsEffect.ExportEmpty)
                is ExportFluidCsv.Result.Failed -> _effects.send(FluidsEffect.ExportFailed)
            }
            localUi.update { it.copy(isExporting = false) }
        }
    }
}
