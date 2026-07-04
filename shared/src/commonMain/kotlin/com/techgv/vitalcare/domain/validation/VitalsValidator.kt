package com.techgv.vitalcare.domain.validation

import com.techgv.vitalcare.core.util.nowLocal
import com.techgv.vitalcare.core.util.todayLocal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

enum class VitalField { TIME, SPO2, HEART_RATE, SYSTOLIC, DIASTOLIC, REMARKS, FORM }

/** Machine-readable reasons; the UI maps them to string resources (D-017). */
enum class VitalsError {
    NOT_A_NUMBER,
    SPO2_OUT_OF_RANGE,
    HEART_RATE_OUT_OF_RANGE,
    SYSTOLIC_OUT_OF_RANGE,
    DIASTOLIC_OUT_OF_RANGE,
    DIASTOLIC_NOT_LESS_THAN_SYSTOLIC,
    BP_PAIR_REQUIRED,
    NO_VITALS,
    REMARKS_TOO_LONG,
    TIME_IN_FUTURE,
}

/** Raw form input; numeric fields arrive as strings straight from the UI. */
data class VitalsInput(
    val date: LocalDate,
    val time: LocalTime,
    val spo2: String = "",
    val heartRate: String = "",
    val systolic: String = "",
    val diastolic: String = "",
    val remarks: String = "",
)

data class ValidatedVitals(
    val time: LocalTime,
    val spo2: Int?,
    val heartRate: Int?,
    val systolic: Int?,
    val diastolic: Int?,
    val remarks: String?,
)

sealed interface ValidationResult {
    data class Valid(val vitals: ValidatedVitals) : ValidationResult
    data class Invalid(val errors: Map<VitalField, VitalsError>) : ValidationResult
}

/**
 * Validation rules from 01 §2: SpO₂ 70–100, HR 20–250, systolic 50–250,
 * diastolic 30–180 and < systolic, BP entered as a pair, at least one vital,
 * remarks ≤ 500 chars, time never in the future (BR-6).
 */
class VitalsValidator(
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    fun validate(input: VitalsInput): ValidationResult {
        val errors = mutableMapOf<VitalField, VitalsError>()

        val spo2 = parseInt(input.spo2, VitalField.SPO2, errors)
        val heartRate = parseInt(input.heartRate, VitalField.HEART_RATE, errors)
        val systolic = parseInt(input.systolic, VitalField.SYSTOLIC, errors)
        val diastolic = parseInt(input.diastolic, VitalField.DIASTOLIC, errors)

        if (spo2 != null && spo2 !in SPO2_RANGE) {
            errors[VitalField.SPO2] = VitalsError.SPO2_OUT_OF_RANGE
        }
        if (heartRate != null && heartRate !in HEART_RATE_RANGE) {
            errors[VitalField.HEART_RATE] = VitalsError.HEART_RATE_OUT_OF_RANGE
        }
        if (systolic != null && systolic !in SYSTOLIC_RANGE) {
            errors[VitalField.SYSTOLIC] = VitalsError.SYSTOLIC_OUT_OF_RANGE
        }
        if (diastolic != null && diastolic !in DIASTOLIC_RANGE) {
            errors[VitalField.DIASTOLIC] = VitalsError.DIASTOLIC_OUT_OF_RANGE
        }

        // BP is a pair (01 §2): flag the missing half.
        val systolicBlank = input.systolic.isBlank()
        val diastolicBlank = input.diastolic.isBlank()
        if (!systolicBlank && diastolicBlank) {
            errors[VitalField.DIASTOLIC] = VitalsError.BP_PAIR_REQUIRED
        }
        if (systolicBlank && !diastolicBlank) {
            errors[VitalField.SYSTOLIC] = VitalsError.BP_PAIR_REQUIRED
        }

        if (systolic != null && diastolic != null &&
            VitalField.SYSTOLIC !in errors && VitalField.DIASTOLIC !in errors &&
            diastolic >= systolic
        ) {
            errors[VitalField.DIASTOLIC] = VitalsError.DIASTOLIC_NOT_LESS_THAN_SYSTOLIC
        }

        if (input.remarks.length > MAX_REMARKS_LENGTH) {
            errors[VitalField.REMARKS] = VitalsError.REMARKS_TOO_LONG
        }

        if (input.date == clock.todayLocal(timeZone) && input.time > clock.nowLocal(timeZone).time) {
            errors[VitalField.TIME] = VitalsError.TIME_IN_FUTURE
        }

        // At least one vital present (01 §2) — only meaningful if the vitals
        // themselves parsed cleanly.
        val anyVital = !input.spo2.isBlank() || !input.heartRate.isBlank() ||
            !systolicBlank || !diastolicBlank
        if (!anyVital) {
            errors[VitalField.FORM] = VitalsError.NO_VITALS
        }

        if (errors.isNotEmpty()) return ValidationResult.Invalid(errors)

        return ValidationResult.Valid(
            ValidatedVitals(
                time = input.time,
                spo2 = spo2,
                heartRate = heartRate,
                systolic = systolic,
                diastolic = diastolic,
                remarks = input.remarks.trim().ifBlank { null },
            ),
        )
    }

    private fun parseInt(
        raw: String,
        field: VitalField,
        errors: MutableMap<VitalField, VitalsError>,
    ): Int? {
        if (raw.isBlank()) return null
        val value = raw.trim().toIntOrNull()
        if (value == null) errors[field] = VitalsError.NOT_A_NUMBER
        return value
    }

    companion object {
        val SPO2_RANGE = 70..100
        val HEART_RATE_RANGE = 20..250
        val SYSTOLIC_RANGE = 50..250
        val DIASTOLIC_RANGE = 30..180
        const val MAX_REMARKS_LENGTH = 500
    }
}
