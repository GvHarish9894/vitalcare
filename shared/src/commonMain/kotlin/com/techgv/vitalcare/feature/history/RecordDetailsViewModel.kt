package com.techgv.vitalcare.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.FullDateFormat
import com.techgv.vitalcare.core.util.TimeFormat
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.usecase.DeleteVitalRecord
import com.techgv.vitalcare.domain.usecase.ObserveVitalRecord
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class RecordDetailsUiState(
    val isLoading: Boolean = true,
    val record: VitalRecord? = null,
    /** BR-2/3: edit/delete only for today's records. */
    val isEditable: Boolean = false,
    val createdText: String = "",
    val updatedText: String = "",
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface RecordDetailsEvent {
    data object DeleteClicked : RecordDetailsEvent
    data object DeleteConfirmed : RecordDetailsEvent
    data object DeleteDismissed : RecordDetailsEvent
}

sealed interface RecordDetailsEffect {
    data object Deleted : RecordDetailsEffect
    data object NotFound : RecordDetailsEffect
    data object DeleteFailed : RecordDetailsEffect
}

class RecordDetailsViewModel(
    private val recordId: String,
    observeVitalRecord: ObserveVitalRecord,
    private val deleteVitalRecord: DeleteVitalRecord,
    private val clock: Clock,
    private val timeZone: TimeZone,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordDetailsUiState())
    val uiState: StateFlow<RecordDetailsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecordDetailsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var deleting = false

    init {
        viewModelScope.launch {
            observeVitalRecord(recordId).collect { record ->
                if (record == null) {
                    // Bad id, or deleted — this VM's own delete already emits
                    // Deleted, so only report the external case.
                    if (!deleting) _effects.send(RecordDetailsEffect.NotFound)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            record = record,
                            isEditable = record.date == clock.todayLocal(timeZone),
                            createdText = formatTimestamp(record.createdAt),
                            updatedText = formatTimestamp(record.updatedAt),
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: RecordDetailsEvent) {
        when (event) {
            RecordDetailsEvent.DeleteClicked ->
                _uiState.update { it.copy(showDeleteDialog = true) }
            RecordDetailsEvent.DeleteDismissed ->
                _uiState.update { it.copy(showDeleteDialog = false) }
            RecordDetailsEvent.DeleteConfirmed -> delete()
        }
    }

    private fun delete() {
        if (_uiState.value.isDeleting) return
        deleting = true
        _uiState.update { it.copy(showDeleteDialog = false, isDeleting = true) }
        viewModelScope.launch {
            when (deleteVitalRecord(recordId)) {
                is AppResult.Success -> _effects.send(RecordDetailsEffect.Deleted)
                is AppResult.Failure -> {
                    deleting = false
                    _uiState.update { it.copy(isDeleting = false) }
                    _effects.send(RecordDetailsEffect.DeleteFailed)
                }
            }
        }
    }

    private fun formatTimestamp(epochMillis: Long): String {
        val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        return "${FullDateFormat.format(dateTime.date)}, ${TimeFormat.format(dateTime.time)}"
    }
}
