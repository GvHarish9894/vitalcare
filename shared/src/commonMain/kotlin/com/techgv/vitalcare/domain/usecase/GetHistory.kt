package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.DateLabel
import com.techgv.vitalcare.core.util.toDateLabel
import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.HistoryFilter
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Clock

data class HistoryDaySection(
    val date: LocalDate,
    val label: DateLabel,
    val records: List<VitalRecord>,
)

/**
 * History list: newest first, grouped under date headers (FR-H1), filtered by
 * range chip (FR-H3), searched against remarks text (FR-H4).
 */
class GetHistory(
    private val repository: VitalsRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    operator fun invoke(filter: HistoryFilter, query: String): Flow<List<HistoryDaySection>> {
        val today = clock.todayLocal(timeZone)
        val source = when (filter) {
            HistoryFilter.ALL -> repository.observeAll()
            HistoryFilter.TODAY -> repository.observeByDate(today)
            HistoryFilter.WEEK ->
                repository.observeByDateRange(today.minus(6, DateTimeUnit.DAY), today)
            HistoryFilter.MONTH ->
                repository.observeByDateRange(today.minus(29, DateTimeUnit.DAY), today)
        }
        val trimmedQuery = query.trim()
        return source.map { records ->
            records
                .filter { record ->
                    trimmedQuery.isBlank() ||
                        record.remarks?.contains(trimmedQuery, ignoreCase = true) == true
                }
                .groupBy { it.date }
                .entries
                .sortedByDescending { it.key }
                .map { (date, dayRecords) ->
                    HistoryDaySection(
                        date = date,
                        label = date.toDateLabel(today),
                        records = dayRecords.sortedByDescending { it.time },
                    )
                }
        }
    }
}
