package com.techgv.vitalcare.feature.fluids

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.nowLocal
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.repository.SettingsRepository
import com.techgv.vitalcare.domain.usecase.GetFluidEntry
import com.techgv.vitalcare.domain.usecase.SaveFluidEntry
import com.techgv.vitalcare.domain.validation.FluidInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class LogFluidViewModel(
    private val entryId: String?,
    initialType: FluidType?,
    private val saveFluidEntry: SaveFluidEntry,
    private val getFluidEntry: GetFluidEntry,
    settingsRepository: SettingsRepository,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    // The display unit is fixed for the duration of this entry (D-032).
    private val unit = settingsRepository.volumeUnit.value

    private val _uiState = MutableStateFlow(
        LogFluidUiState(
            date = clock.todayLocal(timeZone),
            time = clock.nowLocal(timeZone).time.let { LocalTime(it.hour, it.minute) },
            // Pre-selected by the quick-log panel the user came from (03 §3.12).
            type = initialType ?: FluidType.INTAKE,
            unit = unit,
            isEdit = entryId != null,
            isLoading = entryId != null,
        ),
    )
    val uiState: StateFlow<LogFluidUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LogFluidEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var existing: FluidEntry? = null

    init {
        if (entryId != null) loadForEdit(entryId)
    }

    fun onEvent(event: LogFluidEvent) {
        when (event) {
            is LogFluidEvent.TypeChanged -> updateField { it.copy(type = event.type) }
            is LogFluidEvent.AmountChanged ->
                updateField { it.copy(amount = event.value.amountChars()) }
            is LogFluidEvent.PresetSelected ->
                updateField { it.copy(amount = unit.format(event.amountMl)) }
            is LogFluidEvent.NoteChanged ->
                updateField { it.copy(note = event.value.take(NOTE_LIMIT)) }
            is LogFluidEvent.TimeChanged ->
                updateField { it.copy(time = event.time, showTimePicker = false) }
            LogFluidEvent.TimeFieldClicked ->
                _uiState.update { it.copy(showTimePicker = true) }
            LogFluidEvent.TimePickerDismissed ->
                _uiState.update { it.copy(showTimePicker = false) }
            LogFluidEvent.SaveClicked -> save()
            LogFluidEvent.CloseClicked -> close()
            LogFluidEvent.DiscardConfirmed -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                _effects.trySend(LogFluidEffect.Close)
            }
            LogFluidEvent.DiscardDismissed ->
                _uiState.update { it.copy(showDiscardDialog = false) }
        }
    }

    private fun updateField(transform: (LogFluidUiState) -> LogFluidUiState) {
        _uiState.update { transform(it).copy(isDirty = true) }
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            when (val result = getFluidEntry(id)) {
                is AppResult.Success -> {
                    val entry = result.value
                    existing = entry
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            date = entry.date,
                            time = entry.time,
                            type = entry.type,
                            amount = unit.format(entry.amountMl),
                            note = entry.note.orEmpty(),
                            isDirty = false,
                        )
                    }
                }
                is AppResult.Failure -> _effects.send(LogFluidEffect.LoadFailed)
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val input = FluidInput(
                date = state.date,
                time = state.time,
                type = state.type,
                amount = state.amount,
                unit = state.unit,
                note = state.note,
            )
            when (val result = saveFluidEntry(input, existing)) {
                is SaveFluidEntry.Result.Saved -> {
                    _uiState.update { it.copy(isSaving = false, fieldErrors = emptyMap()) }
                    _effects.send(LogFluidEffect.Saved)
                }
                is SaveFluidEntry.Result.Invalid ->
                    _uiState.update { it.copy(isSaving = false, fieldErrors = result.errors) }
                is SaveFluidEntry.Result.Failed -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.send(LogFluidEffect.SaveFailed)
                }
            }
        }
    }

    private fun close() {
        if (_uiState.value.isDirty) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _effects.trySend(LogFluidEffect.Close)
        }
    }

    /** Accept digits plus a single decimal point (oz entry). */
    private fun String.amountChars(): String {
        val filtered = filter { it.isDigit() || it == '.' }
        val firstDot = filtered.indexOf('.')
        val cleaned = if (firstDot == -1) {
            filtered
        } else {
            filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
        }
        return cleaned.take(AMOUNT_LIMIT)
    }

    private companion object {
        const val NOTE_LIMIT = 500
        const val AMOUNT_LIMIT = 7
    }
}
