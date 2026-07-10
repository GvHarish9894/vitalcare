package com.techgv.vitalcare.domain.validation

import com.techgv.vitalcare.core.util.nowLocal
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.VolumeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

enum class FluidField { AMOUNT, TIME, NOTE, FORM }

/** Machine-readable reasons; the UI maps them to string resources (D-017). */
enum class FluidError {
    NOT_A_NUMBER,
    AMOUNT_REQUIRED,
    AMOUNT_OUT_OF_RANGE,
    TIME_IN_FUTURE,
    NOTE_TOO_LONG,
}

/** Raw form input; `amount` arrives as a string in the current display [unit]. */
data class FluidInput(
    val date: LocalDate,
    val time: LocalTime,
    val type: FluidType,
    val amount: String = "",
    val unit: VolumeUnit = VolumeUnit.ML,
    val note: String = "",
)

data class ValidatedFluid(
    val time: LocalTime,
    val type: FluidType,
    val amountMl: Int,
    val note: String?,
)

sealed interface FluidValidationResult {
    data class Valid(val fluid: ValidatedFluid) : FluidValidationResult
    data class Invalid(val errors: Map<FluidField, FluidError>) : FluidValidationResult
}

/**
 * Validation for a fluid entry (01 §2a, D-033): amount required and within
 * 1–5000 mL (after converting from the display unit), note ≤ 500 chars, time
 * never in the future (BR-6). `type` always arrives set from the UI toggle.
 */
class FluidValidator(
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    fun validate(input: FluidInput): FluidValidationResult {
        val errors = mutableMapOf<FluidField, FluidError>()

        var amountMl: Int? = null
        val rawAmount = input.amount.trim()
        if (rawAmount.isBlank()) {
            errors[FluidField.AMOUNT] = FluidError.AMOUNT_REQUIRED
        } else {
            val parsed = rawAmount.toDoubleOrNull()
            if (parsed == null) {
                errors[FluidField.AMOUNT] = FluidError.NOT_A_NUMBER
            } else {
                val ml = input.unit.toMl(parsed)
                if (ml !in AMOUNT_ML_RANGE) {
                    errors[FluidField.AMOUNT] = FluidError.AMOUNT_OUT_OF_RANGE
                } else {
                    amountMl = ml
                }
            }
        }

        if (input.note.length > MAX_NOTE_LENGTH) {
            errors[FluidField.NOTE] = FluidError.NOTE_TOO_LONG
        }

        if (input.date == clock.todayLocal(timeZone) && input.time > clock.nowLocal(timeZone).time) {
            errors[FluidField.TIME] = FluidError.TIME_IN_FUTURE
        }

        if (errors.isNotEmpty() || amountMl == null) {
            return FluidValidationResult.Invalid(errors)
        }

        return FluidValidationResult.Valid(
            ValidatedFluid(
                time = input.time,
                type = input.type,
                amountMl = amountMl,
                note = input.note.trim().ifBlank { null },
            ),
        )
    }

    companion object {
        val AMOUNT_ML_RANGE = 1..5000
        const val MAX_NOTE_LENGTH = 500
    }
}
