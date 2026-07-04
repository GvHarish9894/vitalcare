package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Permanent hard delete (D-025), allowed only for today's records (BR-2 —
 * "today" in the device's current timezone, D-016).
 */
class DeleteVitalRecord(
    private val repository: VitalsRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> {
        val record = when (val result = repository.getById(id)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        if (record.date != clock.todayLocal(timeZone)) {
            return AppResult.Failure(AppError.NotAllowed)
        }
        return repository.delete(id)
    }
}
