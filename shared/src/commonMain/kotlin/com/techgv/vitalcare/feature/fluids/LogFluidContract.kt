package com.techgv.vitalcare.feature.fluids

import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.VolumeUnit
import com.techgv.vitalcare.domain.validation.FluidError
import com.techgv.vitalcare.domain.validation.FluidField
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class LogFluidUiState(
    val date: LocalDate,
    val time: LocalTime,
    val type: FluidType = FluidType.INTAKE,
    val amount: String = "",
    val unit: VolumeUnit = VolumeUnit.ML,
    val note: String = "",
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val fieldErrors: Map<FluidField, FluidError> = emptyMap(),
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showTimePicker: Boolean = false,
)

sealed interface LogFluidEvent {
    data class TypeChanged(val type: FluidType) : LogFluidEvent
    data class AmountChanged(val value: String) : LogFluidEvent
    data class PresetSelected(val amountMl: Int) : LogFluidEvent
    data class NoteChanged(val value: String) : LogFluidEvent
    data class TimeChanged(val time: LocalTime) : LogFluidEvent
    data object TimeFieldClicked : LogFluidEvent
    data object TimePickerDismissed : LogFluidEvent
    data object SaveClicked : LogFluidEvent
    data object CloseClicked : LogFluidEvent
    data object DiscardConfirmed : LogFluidEvent
    data object DiscardDismissed : LogFluidEvent
}

sealed interface LogFluidEffect {
    data object Saved : LogFluidEffect
    data object Close : LogFluidEffect
    data object SaveFailed : LogFluidEffect
    data object LoadFailed : LogFluidEffect
}
