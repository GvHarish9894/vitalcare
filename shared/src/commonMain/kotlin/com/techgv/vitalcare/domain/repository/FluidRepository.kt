package com.techgv.vitalcare.domain.repository

import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.model.FluidEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Room-backed fluid-entry store (D-032) — parallel to [VitalsRepository].
 * Reads are reactive Flows; commands are suspend functions returning
 * [AppResult] (04 §8).
 */
interface FluidRepository {
    fun observeAll(): Flow<List<FluidEntry>>
    fun observeByDate(date: LocalDate): Flow<List<FluidEntry>>
    fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<FluidEntry>>

    suspend fun getById(id: String): AppResult<FluidEntry>
    suspend fun save(entry: FluidEntry): AppResult<Unit>

    /** Permanent hard delete (D-025). */
    suspend fun delete(id: String): AppResult<Unit>

    // Snapshot reads for CSV export / backup (06 §3).
    suspend fun getAll(): AppResult<List<FluidEntry>>
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): AppResult<List<FluidEntry>>

    /** Restore-merge writes (D-024) — upsert only, never deletes. */
    suspend fun upsertAll(entries: List<FluidEntry>): AppResult<Unit>
}
