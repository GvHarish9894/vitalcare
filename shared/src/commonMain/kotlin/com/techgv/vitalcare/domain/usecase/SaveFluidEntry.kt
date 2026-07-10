package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.nowEpochMillis
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.repository.FluidRepository
import com.techgv.vitalcare.domain.validation.FluidField
import com.techgv.vitalcare.domain.validation.FluidError
import com.techgv.vitalcare.domain.validation.FluidInput
import com.techgv.vitalcare.domain.validation.FluidValidationResult
import com.techgv.vitalcare.domain.validation.FluidValidator
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Save path for a fluid entry (D-032), mirroring [SaveVitalRecord]: validate →
 * build/merge → Room. Create stamps a new UUID + today's date (BR-4); edit
 * preserves id/date/createdAt and bumps `updatedAt`.
 */
class SaveFluidEntry(
    private val repository: FluidRepository,
    private val validator: FluidValidator,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    sealed interface Result {
        data object Saved : Result
        data class Invalid(val errors: Map<FluidField, FluidError>) : Result
        data class Failed(val error: AppError) : Result
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(input: FluidInput, existing: FluidEntry? = null): Result {
        val validated = when (val validation = validator.validate(input)) {
            is FluidValidationResult.Invalid -> return Result.Invalid(validation.errors)
            is FluidValidationResult.Valid -> validation.fluid
        }
        val now = clock.nowEpochMillis()
        val entry = existing?.copy(
            time = validated.time,
            type = validated.type,
            amountMl = validated.amountMl,
            note = validated.note,
            updatedAt = now,
        ) ?: FluidEntry(
            id = Uuid.random().toString(),
            date = clock.todayLocal(timeZone),
            time = validated.time,
            type = validated.type,
            amountMl = validated.amountMl,
            note = validated.note,
            createdAt = now,
            updatedAt = now,
        )
        return when (val result = repository.save(entry)) {
            is AppResult.Success -> Result.Saved
            is AppResult.Failure -> Result.Failed(result.error)
        }
    }
}
