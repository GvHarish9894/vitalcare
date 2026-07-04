package com.techgv.vitalcare.domain.model

/** Analytics ranges (FR-AN2). */
enum class AnalyticsRange {
    DAILY,
    WEEKLY,
    MONTHLY,
}

/**
 * One chart point. For DAILY, x = minute-of-day of the reading; for
 * WEEKLY/MONTHLY, x = day offset from the range start and the value is that
 * day's average (06 §7).
 */
data class SeriesPoint(val x: Float, val value: Float)

data class VitalStats(val min: Int, val avg: Int, val max: Int)

/** [stats] is null when the range holds no data for this vital (FR-AN4). */
data class VitalSeries(val points: List<SeriesPoint>, val stats: VitalStats?)

data class AnalyticsData(
    val spo2: VitalSeries,
    val heartRate: VitalSeries,
    val systolic: VitalSeries,
    val diastolic: VitalSeries,
) {
    val isEmpty: Boolean
        get() = spo2.points.isEmpty() && heartRate.points.isEmpty() &&
            systolic.points.isEmpty() && diastolic.points.isEmpty()
}
