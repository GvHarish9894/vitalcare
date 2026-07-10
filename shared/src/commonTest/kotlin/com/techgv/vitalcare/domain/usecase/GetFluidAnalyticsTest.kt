package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.FakeFluidRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.domain.model.AnalyticsRange
import com.techgv.vitalcare.domain.model.FluidType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GetFluidAnalyticsTest {

    private val repository = FakeFluidRepository()
    private val useCase = GetFluidAnalytics(repository, Fixtures.clock, Fixtures.timeZone)

    @Test
    fun dailyPlotsEachEntryAndSumsTotals() = runTest {
        repository.seed(
            Fixtures.fluid(id = "i1", type = FluidType.INTAKE, amountMl = 250, time = LocalTime(8, 0)),
            Fixtures.fluid(id = "i2", type = FluidType.INTAKE, amountMl = 500, time = LocalTime(9, 0)),
            Fixtures.fluid(id = "o1", type = FluidType.OUTPUT, amountMl = 300, time = LocalTime(10, 0)),
        )

        val data = useCase(AnalyticsRange.DAILY).first()

        assertEquals(2, data.intake.size)
        assertEquals(1, data.output.size)
        assertEquals(750, data.totalIntakeMl)
        assertEquals(300, data.totalOutputMl)
        assertEquals(450, data.netMl)
    }

    @Test
    fun weeklyAggregatesPerDayTotalsNotAverages() = runTest {
        repository.seed(
            // Today: 250 + 500 = 750 intake, 300 output.
            Fixtures.fluid(id = "i1", type = FluidType.INTAKE, amountMl = 250),
            Fixtures.fluid(id = "i2", type = FluidType.INTAKE, amountMl = 500),
            Fixtures.fluid(id = "o1", type = FluidType.OUTPUT, amountMl = 300),
            // Yesterday: 200 intake.
            Fixtures.fluid(id = "y1", date = Fixtures.yesterday, type = FluidType.INTAKE, amountMl = 200),
        )

        val data = useCase(AnalyticsRange.WEEKLY).first()

        // Daily SUMS, not averages: today's intake point is 750 (not 375).
        assertEquals(listOf(200f, 750f), data.intake.map { it.value })
        assertEquals(listOf(300f), data.output.map { it.value })
        // Net per day = intake − output.
        assertEquals(listOf(200f, 450f), data.net.map { it.value })
        // Range totals.
        assertEquals(950, data.totalIntakeMl)
        assertEquals(300, data.totalOutputMl)
    }

    @Test
    fun emptyRangeIsEmpty() = runTest {
        assertEquals(true, useCase(AnalyticsRange.WEEKLY).first().isEmpty)
    }
}
