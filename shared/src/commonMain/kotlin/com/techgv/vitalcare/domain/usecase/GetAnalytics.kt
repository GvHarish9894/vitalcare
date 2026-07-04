package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.AnalyticsData
import com.techgv.vitalcare.domain.model.AnalyticsRange
import com.techgv.vitalcare.domain.model.SeriesPoint
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.model.VitalSeries
import com.techgv.vitalcare.domain.model.VitalStats
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * Pure aggregation for the Analytics screen (FR-AN1..AN3, 06 §7): daily =
 * today's readings by time-of-day; weekly/monthly = per-day averages over the
 * last 7/30 days.
 */
class GetAnalytics(
    private val repository: VitalsRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    operator fun invoke(range: AnalyticsRange): Flow<AnalyticsData> {
        val today = clock.todayLocal(timeZone)
        return when (range) {
            AnalyticsRange.DAILY ->
                repository.observeByDate(today).map { buildDaily(it) }
            AnalyticsRange.WEEKLY -> {
                val start = today.minus(6, DateTimeUnit.DAY)
                repository.observeByDateRange(start, today).map { buildAveraged(it, start) }
            }
            AnalyticsRange.MONTHLY -> {
                val start = today.minus(29, DateTimeUnit.DAY)
                repository.observeByDateRange(start, today).map { buildAveraged(it, start) }
            }
        }
    }

    private fun buildDaily(records: List<VitalRecord>): AnalyticsData {
        val sorted = records.sortedBy { it.time }
        fun series(selector: (VitalRecord) -> Int?): VitalSeries {
            val points = sorted.mapNotNull { record ->
                selector(record)?.let { value ->
                    SeriesPoint(
                        x = (record.time.hour * 60 + record.time.minute).toFloat(),
                        value = value.toFloat(),
                    )
                }
            }
            return VitalSeries(points, stats(points))
        }
        return AnalyticsData(
            spo2 = series { it.spo2 },
            heartRate = series { it.heartRate },
            systolic = series { it.systolic },
            diastolic = series { it.diastolic },
        )
    }

    private fun buildAveraged(records: List<VitalRecord>, start: LocalDate): AnalyticsData {
        val byDay = records.groupBy { it.date }
        fun series(selector: (VitalRecord) -> Int?): VitalSeries {
            val points = byDay.entries
                .sortedBy { it.key }
                .mapNotNull { (date, dayRecords) ->
                    val values = dayRecords.mapNotNull(selector)
                    if (values.isEmpty()) return@mapNotNull null
                    SeriesPoint(
                        x = start.daysUntil(date).toFloat(),
                        value = (values.sum().toFloat() / values.size),
                    )
                }
            return VitalSeries(points, stats(points))
        }
        return AnalyticsData(
            spo2 = series { it.spo2 },
            heartRate = series { it.heartRate },
            systolic = series { it.systolic },
            diastolic = series { it.diastolic },
        )
    }

    private fun stats(points: List<SeriesPoint>): VitalStats? {
        if (points.isEmpty()) return null
        val values = points.map { it.value }
        return VitalStats(
            min = values.min().roundToInt(),
            avg = (values.sum() / values.size).roundToInt(),
            max = values.max().roundToInt(),
        )
    }
}
