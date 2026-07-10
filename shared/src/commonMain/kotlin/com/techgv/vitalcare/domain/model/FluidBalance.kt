package com.techgv.vitalcare.domain.model

/**
 * Today's fluid balance for the Dashboard card and Fluids hub (FR-FL2, D-033).
 * All amounts are canonical mL; the UI converts to the display unit.
 */
data class FluidDayBalance(
    val intakeMl: Int,
    val outputMl: Int,
    val goalMl: Int,
    val entries: List<FluidEntry>,
) {
    /** Net balance = intake − output (can be negative). */
    val netMl: Int get() = intakeMl - outputMl

    /** Progress toward the intake goal, clamped to 0..1 (0 when no goal set). */
    val goalProgress: Float
        get() = if (goalMl > 0) (intakeMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f

    val isEmpty: Boolean get() = entries.isEmpty()
}
