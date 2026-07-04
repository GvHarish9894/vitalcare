package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.FakeVitalsRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.domain.model.AnalyticsRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetAnalyticsTest {

    private val repository = FakeVitalsRepository()
    private val getAnalytics = GetAnalytics(repository, Fixtures.clock, Fixtures.timeZone)

    @Test
    fun emptyRangeYieldsEmptySeriesAndNullStats() = runTest {
        val data = getAnalytics(AnalyticsRange.DAILY).first()

        assertTrue(data.isEmpty)
        assertTrue(data.spo2.points.isEmpty())
        assertNull(data.spo2.stats)
    }

    @Test
    fun dailyMapsReadingsToMinuteOfDayInTimeOrder() = runTest {
        repository.seed(
            Fixtures.record(id = "a", time = LocalTime(9, 30), heartRate = 80),
            Fixtures.record(id = "b", time = LocalTime(8, 0), heartRate = 70),
        )

        val data = getAnalytics(AnalyticsRange.DAILY).first()

        assertEquals(2, data.heartRate.points.size)
        assertEquals(480f, data.heartRate.points[0].x) // 08:00
        assertEquals(70f, data.heartRate.points[0].value)
        assertEquals(570f, data.heartRate.points[1].x) // 09:30
    }

    @Test
    fun singlePointRangeHasEqualMinAvgMax() = runTest {
        repository.seed(Fixtures.record(id = "a", spo2 = 97))

        val stats = getAnalytics(AnalyticsRange.DAILY).first().spo2.stats

        assertEquals(97, stats?.min)
        assertEquals(97, stats?.avg)
        assertEquals(97, stats?.max)
    }

    @Test
    fun weeklyAveragesPerDayAndOffsetsFromRangeStart() = runTest {
        // Two readings yesterday (avg 75), one today (90).
        repository.seed(
            Fixtures.record(id = "a", date = Fixtures.yesterday, heartRate = 70),
            Fixtures.record(id = "b", date = Fixtures.yesterday, time = LocalTime(9, 0), heartRate = 80),
            Fixtures.record(id = "c", date = Fixtures.today, heartRate = 90),
        )

        val series = getAnalytics(AnalyticsRange.WEEKLY).first().heartRate

        assertEquals(2, series.points.size)
        assertEquals(5f, series.points[0].x) // yesterday = day 5 of 0..6
        assertEquals(75f, series.points[0].value)
        assertEquals(6f, series.points[1].x) // today
        assertEquals(90f, series.points[1].value)
        assertEquals(75, series.stats?.min)
        assertEquals(83, series.stats?.avg) // (75+90)/2 = 82.5 → 83
        assertEquals(90, series.stats?.max)
    }

    @Test
    fun weeklyExcludesRecordsOlderThanSevenDays() = runTest {
        repository.seed(
            Fixtures.record(id = "old", date = LocalDate(2026, 6, 20), heartRate = 65),
            Fixtures.record(id = "new", date = Fixtures.today, heartRate = 90),
        )

        val series = getAnalytics(AnalyticsRange.WEEKLY).first().heartRate

        assertEquals(1, series.points.size)
        assertEquals(90f, series.points.single().value)
    }

    @Test
    fun nullVitalsAreSkippedNotZero() = runTest {
        repository.seed(
            Fixtures.record(id = "a", spo2 = null, heartRate = 72),
        )

        val data = getAnalytics(AnalyticsRange.DAILY).first()

        assertTrue(data.spo2.points.isEmpty())
        assertEquals(1, data.heartRate.points.size)
    }
}
