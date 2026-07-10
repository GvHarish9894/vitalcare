package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.AnalyticsRange
import com.techgv.vitalcare.domain.model.FluidAnalyticsData
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.model.SeriesPoint
import com.techgv.vitalcare.domain.repository.FluidRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.time.Clock

/**
 * Fluid aggregation for the Fluids trend (FR-FL5, 06 §7, D-032). DAILY = today's
 * entries by time-of-day; WEEKLY/MONTHLY = per-day **totals** (sum) over the
 * last 7/30 days. This is the sum aggregation that vitals' [GetAnalytics]
 * deliberately does not do.
 */
class GetFluidAnalytics(
    private val repository: FluidRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {

    operator fun invoke(range: AnalyticsRange): Flow<FluidAnalyticsData> {
        val today = clock.todayLocal(timeZone)
        return when (range) {
            AnalyticsRange.DAILY ->
                repository.observeByDate(today).map { buildDaily(it, range) }
            AnalyticsRange.WEEKLY -> {
                val start = today.minus(6, DateTimeUnit.DAY)
                repository.observeByDateRange(start, today).map { buildTotals(it, start, range) }
            }
            AnalyticsRange.MONTHLY -> {
                val start = today.minus(29, DateTimeUnit.DAY)
                repository.observeByDateRange(start, today).map { buildTotals(it, start, range) }
            }
        }
    }

    private fun buildDaily(entries: List<FluidEntry>, range: AnalyticsRange): FluidAnalyticsData {
        fun points(type: FluidType): List<SeriesPoint> =
            entries.filter { it.type == type }
                .sortedBy { it.time }
                .map {
                    SeriesPoint(
                        x = (it.time.hour * 60 + it.time.minute).toFloat(),
                        value = it.amountMl.toFloat(),
                    )
                }
        val intake = points(FluidType.INTAKE)
        val output = points(FluidType.OUTPUT)
        return FluidAnalyticsData(
            range = range,
            intake = intake,
            output = output,
            net = emptyList(), // net-per-event isn't meaningful for the daily view
            totalIntakeMl = intake.sumOf { it.value.toInt() },
            totalOutputMl = output.sumOf { it.value.toInt() },
        )
    }

    private fun buildTotals(
        entries: List<FluidEntry>,
        start: LocalDate,
        range: AnalyticsRange,
    ): FluidAnalyticsData {
        val byDay = entries.groupBy { it.date }
        val days = byDay.keys.sorted()

        fun dayTotal(date: LocalDate, type: FluidType): Int =
            byDay[date].orEmpty().filter { it.type == type }.sumOf { it.amountMl }

        val intake = mutableListOf<SeriesPoint>()
        val output = mutableListOf<SeriesPoint>()
        val net = mutableListOf<SeriesPoint>()
        for (date in days) {
            val x = start.daysUntil(date).toFloat()
            val inMl = dayTotal(date, FluidType.INTAKE)
            val outMl = dayTotal(date, FluidType.OUTPUT)
            if (inMl > 0) intake += SeriesPoint(x, inMl.toFloat())
            if (outMl > 0) output += SeriesPoint(x, outMl.toFloat())
            net += SeriesPoint(x, (inMl - outMl).toFloat())
        }
        return FluidAnalyticsData(
            range = range,
            intake = intake,
            output = output,
            net = net,
            totalIntakeMl = entries.filter { it.type == FluidType.INTAKE }.sumOf { it.amountMl },
            totalOutputMl = entries.filter { it.type == FluidType.OUTPUT }.sumOf { it.amountMl },
        )
    }
}
