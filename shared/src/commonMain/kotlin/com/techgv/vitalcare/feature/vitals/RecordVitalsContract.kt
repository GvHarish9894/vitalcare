package com.techgv.vitalcare.feature.vitals

import com.techgv.vitalcare.domain.validation.VitalField
import com.techgv.vitalcare.domain.validation.VitalsError
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class RecordVitalsUiState(
    val date: LocalDate,
    val time: LocalTime,
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val spo2: String = "",
    val heartRate: String = "",
    val systolic: String = "",
    val diastolic: String = "",
    val remarks: String = "",
    val fieldErrors: Map<VitalField, VitalsError> = emptyMap(),
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showTimePicker: Boolean = false,
)

sealed interface RecordVitalsEvent {
    data class Spo2Changed(val value: String) : RecordVitalsEvent
    data class HeartRateChanged(val value: String) : RecordVitalsEvent
    data class SystolicChanged(val value: String) : RecordVitalsEvent
    data class DiastolicChanged(val value: String) : RecordVitalsEvent
    data class RemarksChanged(val value: String) : RecordVitalsEvent
    data class TimeChanged(val time: LocalTime) : RecordVitalsEvent
    data object TimeFieldClicked : RecordVitalsEvent
    data object TimePickerDismissed : RecordVitalsEvent
    data object SaveClicked : RecordVitalsEvent
    data object CloseClicked : RecordVitalsEvent
    data object DiscardConfirmed : RecordVitalsEvent
    data object DiscardDismissed : RecordVitalsEvent
}

sealed interface RecordVitalsEffect {
    /** Saved successfully — show confirmation and pop back (FR-R3). */
    data object Saved : RecordVitalsEffect

    /** Leave without saving. */
    data object Close : RecordVitalsEffect

    /** Save hit an unexpected storage error. */
    data object SaveFailed : RecordVitalsEffect

    /** Edit target could not be loaded. */
    data object LoadFailed : RecordVitalsEffect
}
