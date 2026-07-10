package com.techgv.vitalcare

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.repository.FluidRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class FakeFluidRepository : FluidRepository {

    private val entries = MutableStateFlow<Map<String, FluidEntry>>(emptyMap())
    var failWrites: Boolean = false

    fun seed(vararg items: FluidEntry) {
        entries.value = items.associateBy { it.id }
    }

    fun current(): List<FluidEntry> = entries.value.values.toList()

    override fun observeAll(): Flow<List<FluidEntry>> = entries.map { map ->
        map.values.sortedWith(
            compareByDescending<FluidEntry> { it.date }.thenByDescending { it.time },
        )
    }

    override fun observeByDate(date: LocalDate): Flow<List<FluidEntry>> =
        observeAll().map { list -> list.filter { it.date == date } }

    override fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<FluidEntry>> =
        observeAll().map { list -> list.filter { it.date in from..to } }

    override suspend fun getById(id: String): AppResult<FluidEntry> =
        entries.value[id]?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.NotFound)

    override suspend fun save(entry: FluidEntry): AppResult<Unit> {
        if (failWrites) return AppResult.Failure(AppError.Unknown())
        entries.update { it + (entry.id to entry) }
        return AppResult.Success(Unit)
    }

    override suspend fun delete(id: String): AppResult<Unit> {
        if (failWrites) return AppResult.Failure(AppError.Unknown())
        entries.update { it - id }
        return AppResult.Success(Unit)
    }

    override suspend fun getAll(): AppResult<List<FluidEntry>> = AppResult.Success(
        entries.value.values.sortedWith(compareBy<FluidEntry> { it.date }.thenBy { it.time }),
    )

    override suspend fun getByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<List<FluidEntry>> = AppResult.Success(
        entries.value.values
            .filter { it.date in from..to }
            .sortedWith(compareBy<FluidEntry> { it.date }.thenBy { it.time }),
    )

    override suspend fun upsertAll(entries: List<FluidEntry>): AppResult<Unit> {
        if (failWrites) return AppResult.Failure(AppError.Unknown())
        this.entries.update { it + entries.associateBy { entry -> entry.id } }
        return AppResult.Success(Unit)
    }
}
