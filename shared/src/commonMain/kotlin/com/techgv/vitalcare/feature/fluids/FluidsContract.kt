package com.techgv.vitalcare.feature.fluids

import com.techgv.vitalcare.domain.model.AnalyticsRange
import com.techgv.vitalcare.domain.model.FluidAnalyticsData
import com.techgv.vitalcare.domain.model.FluidDayBalance
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.VolumeUnit

data class FluidsUiState(
    val unit: VolumeUnit = VolumeUnit.ML,
    val balance: FluidDayBalance? = null,
    val range: AnalyticsRange = AnalyticsRange.WEEKLY,
    val analytics: FluidAnalyticsData? = null,
    val isExporting: Boolean = false,
    val showExportChooser: Boolean = false,
    val deleteTargetId: String? = null,
)

sealed interface FluidsEvent {
    /** One-tap quick log — the type comes from the panel tapped, never a mode (03 §3.11). */
    data class QuickAdd(val type: FluidType, val amountMl: Int) : FluidsEvent
    data class RangeChanged(val range: AnalyticsRange) : FluidsEvent
    data class DeleteRequested(val id: String) : FluidsEvent
    data object DeleteConfirmed : FluidsEvent
    data object DeleteDismissed : FluidsEvent
    data object ExportClicked : FluidsEvent
    data class ExportScopeSelected(val scope: HistoryFilter) : FluidsEvent
    data object ExportDismissed : FluidsEvent
}

sealed interface FluidsEffect {
    data class Logged(val type: FluidType) : FluidsEffect
    data object Deleted : FluidsEffect
    data object SaveFailed : FluidsEffect
    data object ExportEmpty : FluidsEffect
    data object ExportFailed : FluidsEffect
}
