package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.nowEpochMillis
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.repository.VitalsRepository
import com.techgv.vitalcare.domain.validation.ValidationResult
import com.techgv.vitalcare.domain.validation.VitalField
import com.techgv.vitalcare.domain.validation.VitalsError
import com.techgv.vitalcare.domain.validation.VitalsInput
import com.techgv.vitalcare.domain.validation.VitalsValidator
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The canonical local save path (04 §9): validate → build/merge the record →
 * Room. Create mode stamps a new UUID + today's date (BR-4); edit mode
 * preserves id/date/createdAt and bumps `updatedAt` (FR-R4).
 */
class SaveVitalRecord(
    private val repository: VitalsRepository,
    private val validator: VitalsValidator,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    sealed interface Result {
        data object Saved : Result
        data class Invalid(val errors: Map<VitalField, VitalsError>) : Result
        data class Failed(val error: AppError) : Result
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(input: VitalsInput, existing: VitalRecord? = null): Result {
        val validated = when (val validation = validator.validate(input)) {
            is ValidationResult.Invalid -> return Result.Invalid(validation.errors)
            is ValidationResult.Valid -> validation.vitals
        }
        val now = clock.nowEpochMillis()
        val record = existing?.copy(
            time = validated.time,
            spo2 = validated.spo2,
            heartRate = validated.heartRate,
            systolic = validated.systolic,
            diastolic = validated.diastolic,
            remarks = validated.remarks,
            updatedAt = now,
        ) ?: VitalRecord(
            id = Uuid.random().toString(),
            date = clock.todayLocal(timeZone),
            time = validated.time,
            spo2 = validated.spo2,
            heartRate = validated.heartRate,
            systolic = validated.systolic,
            diastolic = validated.diastolic,
            remarks = validated.remarks,
            createdAt = now,
            updatedAt = now,
        )
        return when (val result = repository.save(record)) {
            is AppResult.Success -> Result.Saved
            is AppResult.Failure -> Result.Failed(result.error)
        }
    }
}
