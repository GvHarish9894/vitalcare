package com.techgv.vitalcare.domain.usecase

import com.techgv.vitalcare.core.util.todayLocal
import com.techgv.vitalcare.domain.model.FluidDayBalance
import com.techgv.vitalcare.domain.model.FluidType
import com.techgv.vitalcare.domain.repository.FluidRepository
import com.techgv.vitalcare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Reactive fluid balance for today (FR-FL2): intake/output totals, net balance,
 * and goal progress. Sums today's entries — the fluid aggregation is by SUM,
 * not average (D-032).
 */
class GetFluidBalanceToday(
    private val repository: FluidRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    operator fun invoke(): Flow<FluidDayBalance> = combine(
        repository.observeByDate(clock.todayLocal(timeZone)),
        settings.dailyFluidGoalMl,
    ) { entries, goalMl ->
        FluidDayBalance(
            intakeMl = entries.filter { it.type == FluidType.INTAKE }.sumOf { it.amountMl },
            outputMl = entries.filter { it.type == FluidType.OUTPUT }.sumOf { it.amountMl },
            goalMl = goalMl,
            entries = entries,
        )
    }
}
