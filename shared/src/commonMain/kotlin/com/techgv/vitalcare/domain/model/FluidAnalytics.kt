package com.techgv.vitalcare.domain.model

/**
 * Fluid analytics (FR-FL5, D-033). Unlike vitals analytics (which averages),
 * fluids aggregate by **sum**: for DAILY each point is one entry by
 * time-of-day; for WEEKLY/MONTHLY each point is that day's total. `net` is the
 * per-point intake − output. Amounts are canonical mL. Reuses [SeriesPoint].
 */
data class FluidAnalyticsData(
    val range: AnalyticsRange,
    val intake: List<SeriesPoint>,
    val output: List<SeriesPoint>,
    val net: List<SeriesPoint>,
    val totalIntakeMl: Int,
    val totalOutputMl: Int,
) {
    val netMl: Int get() = totalIntakeMl - totalOutputMl
    val isEmpty: Boolean get() = intake.isEmpty() && output.isEmpty()
}
