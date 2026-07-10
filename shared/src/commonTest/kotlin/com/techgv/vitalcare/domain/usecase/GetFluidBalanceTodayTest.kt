package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.FakeFluidRepository
import com.techgv.vitalcare.FakeSettingsRepository
import com.techgv.vitalcare.Fixtures
import com.techgv.vitalcare.domain.model.FluidType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetFluidBalanceTodayTest {

    private val repository = FakeFluidRepository()
    private val settings = FakeSettingsRepository()
    private val useCase = GetFluidBalanceToday(repository, settings, Fixtures.clock, Fixtures.timeZone)

    @Test
    fun sumsTodayIntakeAndOutputWithNet() = runTest {
        settings.setDailyFluidGoalMl(2000)
        repository.seed(
            Fixtures.fluid(id = "i1", type = FluidType.INTAKE, amountMl = 250),
            Fixtures.fluid(id = "i2", type = FluidType.INTAKE, amountMl = 500),
            Fixtures.fluid(id = "o1", type = FluidType.OUTPUT, amountMl = 300),
            // A different day's entry must be ignored.
            Fixtures.fluid(id = "old", date = Fixtures.yesterday, amountMl = 999),
        )

        val balance = useCase().first()

        assertEquals(750, balance.intakeMl)
        assertEquals(300, balance.outputMl)
        assertEquals(450, balance.netMl)
        assertEquals(3, balance.entries.size)
    }

    @Test
    fun goalProgressIsClampedFraction() = runTest {
        settings.setDailyFluidGoalMl(1000)
        repository.seed(Fixtures.fluid(id = "i1", type = FluidType.INTAKE, amountMl = 250))

        assertEquals(0.25f, useCase().first().goalProgress)
    }

    @Test
    fun goalProgressNeverExceedsOne() = runTest {
        settings.setDailyFluidGoalMl(1000)
        repository.seed(Fixtures.fluid(id = "i1", type = FluidType.INTAKE, amountMl = 3000))

        assertEquals(1f, useCase().first().goalProgress)
    }

    @Test
    fun emptyDayReportsEmptyBalance() = runTest {
        val balance = useCase().first()
        assertEquals(0, balance.intakeMl)
        assertEquals(0, balance.outputMl)
        assertEquals(true, balance.isEmpty)
    }
}
