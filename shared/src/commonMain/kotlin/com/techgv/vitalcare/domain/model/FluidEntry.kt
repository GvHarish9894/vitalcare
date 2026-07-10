package com.techgv.vitalcare.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * A single fluid-balance event (06 §1, D-032) — a separate concept from
 * [VitalRecord]. Each entry is one discrete intake or output; the app sums
 * them per day into totals and a net balance. `amountMl` is always canonical
 * millilitres (the display unit is a presentation-only preference). `date` is
 * fixed at creation (BR-4); `updatedAt` drives restore-merge LWW (D-024) and is
 * epoch millis UTC (D-016).
 */
data class FluidEntry(
    val id: String,
    val date: LocalDate,
    val time: LocalTime,
    val type: FluidType,
    val amountMl: Int,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Direction of a fluid event. `OUTPUT` is urine (D-032). */
enum class FluidType { INTAKE, OUTPUT }
