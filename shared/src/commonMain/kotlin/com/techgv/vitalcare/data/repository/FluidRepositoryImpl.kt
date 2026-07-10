package com.techgv.vitalcare.data.repository

import com.techgv.vitalcare.core.util.AppError
import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.core.util.DispatcherProvider
import com.techgv.vitalcare.data.local.FluidEntryDao
import com.techgv.vitalcare.domain.model.FluidEntry
import com.techgv.vitalcare.domain.repository.FluidRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class FluidRepositoryImpl(
    private val dao: FluidEntryDao,
    private val dispatchers: DispatcherProvider,
) : FluidRepository {

    override fun observeAll(): Flow<List<FluidEntry>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeByDate(date: LocalDate): Flow<List<FluidEntry>> =
        dao.observeByDate(date.toString()).map { entities -> entities.map { it.toDomain() } }

    override fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<FluidEntry>> =
        dao.observeByDateRange(from.toString(), to.toString())
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<FluidEntry> = guard {
        dao.getById(id)?.toDomain()
    }.let { result ->
        when (result) {
            is AppResult.Success ->
                result.value?.let { AppResult.Success(it) }
                    ?: AppResult.Failure(AppError.NotFound)
            is AppResult.Failure -> result
        }
    }

    override suspend fun save(entry: FluidEntry): AppResult<Unit> = guard {
        dao.upsert(entry.toEntity())
    }

    override suspend fun delete(id: String): AppResult<Unit> = guard {
        dao.hardDelete(id)
    }

    override suspend fun getAll(): AppResult<List<FluidEntry>> = guard {
        dao.getAll().map { it.toDomain() }
    }

    override suspend fun getByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<List<FluidEntry>> = guard {
        dao.getByDateRange(from.toString(), to.toString()).map { it.toDomain() }
    }

    override suspend fun upsertAll(entries: List<FluidEntry>): AppResult<Unit> = guard {
        dao.upsertAll(entries.map { it.toEntity() })
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
