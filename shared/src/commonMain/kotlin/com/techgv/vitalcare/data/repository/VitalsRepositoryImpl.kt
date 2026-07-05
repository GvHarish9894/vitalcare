package com.techgv.vitalcare.data.repository

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.DispatcherProvider
import com.techgv.vitalcare.data.local.VitalRecordDao
import com.techgv.vitalcare.domain.model.VitalRecord
import com.techgv.vitalcare.domain.repository.VitalsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class VitalsRepositoryImpl(
    private val dao: VitalRecordDao,
    private val dispatchers: DispatcherProvider,
) : VitalsRepository {

    override fun observeAll(): Flow<List<VitalRecord>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeByDate(date: LocalDate): Flow<List<VitalRecord>> =
        dao.observeByDate(date.toString()).map { entities -> entities.map { it.toDomain() } }

    override fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<VitalRecord>> =
        dao.observeByDateRange(from.toString(), to.toString())
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<VitalRecord> = guard {
        dao.getById(id)?.toDomain()
    }.let { result ->
        when (result) {
            is AppResult.Success ->
                result.value?.let { AppResult.Success(it) }
                    ?: AppResult.Failure(AppError.NotFound)
            is AppResult.Failure -> result
        }
    }

    override suspend fun save(record: VitalRecord): AppResult<Unit> = guard {
        dao.upsert(record.toEntity())
    }

    override suspend fun delete(id: String): AppResult<Unit> = guard {
        dao.hardDelete(id)
    }

    override suspend fun getAll(): AppResult<List<VitalRecord>> = guard {
        dao.getAll().map { it.toDomain() }
    }

    override suspend fun getByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<List<VitalRecord>> = guard {
        dao.getByDateRange(from.toString(), to.toString()).map { it.toDomain() }
    }

    override suspend fun upsertAll(records: List<VitalRecord>): AppResult<Unit> = guard {
        dao.upsertAll(records.map { it.toEntity() })
    }

    /** Exceptions stop at the data layer and become [AppError]s (04 §8). */
    private suspend fun <T> guard(block: suspend () -> T): AppResult<T> =
        withContext(dispatchers.io) {
            try {
                AppResult.Success(block())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                AppResult.Failure(AppError.Unknown(t))
            }
        }
}
