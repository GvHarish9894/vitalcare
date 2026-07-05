package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

data class TodaySummary(
    val count: Int,
    val latest: VitalRecord?,
    val times: List<LocalTime>,
)

/** Reactive today-summary for the Dashboard (FR-D1/D2/D5). */
class GetTodaySummary(
    private val repository: VitalsRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    operator fun invoke(): Flow<TodaySummary> =
        repository.observeByDate(clock.todayLocal(timeZone)).map { records ->
            // observeByDate emits newest-first.
            TodaySummary(
                count = records.size,
                latest = records.firstOrNull(),
                times = records.map { it.time },
            )
        }
}
