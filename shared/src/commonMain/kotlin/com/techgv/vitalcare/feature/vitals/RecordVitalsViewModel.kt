package com.techgv.vitalcare.feature.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.nowLocal
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.usecase.GetVitalRecord
import com.techgv.vitalcare.domain.usecase.SaveVitalRecord
import com.techgv.vitalcare.domain.validation.VitalsInput
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

class RecordVitalsViewModel(
    private val recordId: String?,
    private val saveVitalRecord: SaveVitalRecord,
    private val getVitalRecord: GetVitalRecord,
    clock: Clock,
    timeZone: TimeZone,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RecordVitalsUiState(
            date = clock.todayLocal(timeZone),
            time = clock.nowLocal(timeZone).time.let { LocalTime(it.hour, it.minute) },
            isEdit = recordId != null,
            isLoading = recordId != null,
        ),
    )
    val uiState: StateFlow<RecordVitalsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecordVitalsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var existing: VitalRecord? = null

    init {
        if (recordId != null) loadForEdit(recordId)
    }

    fun onEvent(event: RecordVitalsEvent) {
        when (event) {
            is RecordVitalsEvent.Spo2Changed ->
                updateField { it.copy(spo2 = event.value.digits(3)) }
            is RecordVitalsEvent.HeartRateChanged ->
                updateField { it.copy(heartRate = event.value.digits(3)) }
            is RecordVitalsEvent.SystolicChanged ->
                updateField { it.copy(systolic = event.value.digits(3)) }
            is RecordVitalsEvent.DiastolicChanged ->
                updateField { it.copy(diastolic = event.value.digits(3)) }
            is RecordVitalsEvent.RemarksChanged ->
                updateField { it.copy(remarks = event.value.take(REMARKS_LIMIT)) }
            is RecordVitalsEvent.TimeChanged ->
                updateField { it.copy(time = event.time, showTimePicker = false) }
            RecordVitalsEvent.TimeFieldClicked ->
                _uiState.update { it.copy(showTimePicker = true) }
            RecordVitalsEvent.TimePickerDismissed ->
                _uiState.update { it.copy(showTimePicker = false) }
            RecordVitalsEvent.SaveClicked -> save()
            RecordVitalsEvent.CloseClicked -> close()
            RecordVitalsEvent.DiscardConfirmed -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                _effects.trySend(RecordVitalsEffect.Close)
            }
            RecordVitalsEvent.DiscardDismissed ->
                _uiState.update { it.copy(showDiscardDialog = false) }
        }
    }

    private fun updateField(transform: (RecordVitalsUiState) -> RecordVitalsUiState) {
        _uiState.update { transform(it).copy(isDirty = true) }
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            when (val result = getVitalRecord(id)) {
                is AppResult.Success -> {
                    val record = result.value
                    existing = record
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            date = record.date,
                            time = record.time,
                            spo2 = record.spo2?.toString().orEmpty(),
                            heartRate = record.heartRate?.toString().orEmpty(),
                            systolic = record.systolic?.toString().orEmpty(),
                            diastolic = record.diastolic?.toString().orEmpty(),
                            remarks = record.remarks.orEmpty(),
                            isDirty = false,
                        )
                    }
                }
                is AppResult.Failure -> _effects.send(RecordVitalsEffect.LoadFailed)
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val input = VitalsInput(
                date = state.date,
                time = state.time,
                spo2 = state.spo2,
                heartRate = state.heartRate,
                systolic = state.systolic,
                diastolic = state.diastolic,
                remarks = state.remarks,
            )
            when (val result = saveVitalRecord(input, existing)) {
                is SaveVitalRecord.Result.Saved -> {
                    _uiState.update { it.copy(isSaving = false, fieldErrors = emptyMap()) }
                    _effects.send(RecordVitalsEffect.Saved)
                }
                is SaveVitalRecord.Result.Invalid ->
                    _uiState.update { it.copy(isSaving = false, fieldErrors = result.errors) }
                is SaveVitalRecord.Result.Failed -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.send(RecordVitalsEffect.SaveFailed)
                }
            }
        }
    }

    private fun close() {
        if (_uiState.value.isDirty) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _effects.trySend(RecordVitalsEffect.Close)
        }
    }

    private fun String.digits(maxLength: Int): String =
        filter { it.isDigit() }.take(maxLength)

    private companion object {
        const val REMARKS_LIMIT = 500
    }
}
