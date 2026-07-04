package com.techgv.vitalcare

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class FakeVitalsRepository : VitalsRepository {

    private val records = MutableStateFlow<Map<String, VitalRecord>>(emptyMap())
    var failWrites: Boolean = false

    fun seed(vararg items: VitalRecord) {
        records.value = items.associateBy { it.id }
    }

    fun current(): List<VitalRecord> = records.value.values.toList()

    override fun observeAll(): Flow<List<VitalRecord>> = records.map { map ->
        map.values.sortedWith(
            compareByDescending<VitalRecord> { it.date }.thenByDescending { it.time },
        )
    }

    override fun observeByDate(date: LocalDate): Flow<List<VitalRecord>> =
        observeAll().map { list -> list.filter { it.date == date } }

    override fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<VitalRecord>> =
        observeAll().map { list -> list.filter { it.date in from..to } }

    override suspend fun getById(id: String): AppResult<VitalRecord> =
        records.value[id]?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.NotFound)

    override suspend fun save(record: VitalRecord): AppResult<Unit> {
        if (failWrites) return AppResult.Failure(AppError.Unknown())
        records.update { it + (record.id to record) }
        return AppResult.Success(Unit)
    }

    override suspend fun delete(id: String): AppResult<Unit> {
        if (failWrites) return AppResult.Failure(AppError.Unknown())
        records.update { it - id }
        return AppResult.Success(Unit)
    }

    override suspend fun getAll(): AppResult<List<VitalRecord>> = AppResult.Success(
        records.value.values.sortedWith(compareBy<VitalRecord> { it.date }.thenBy { it.time }),
    )

    override suspend fun getByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<List<VitalRecord>> = AppResult.Success(
        records.value.values
            .filter { it.date in from..to }
            .sortedWith(compareBy<VitalRecord> { it.date }.thenBy { it.time }),
    )
}
