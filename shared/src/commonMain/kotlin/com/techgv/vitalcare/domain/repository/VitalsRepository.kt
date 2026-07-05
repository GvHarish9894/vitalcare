package com.techgv.vitalcare.domain.repository

import com.techgv.vitalcare.core.util.AppResult
import com.techgv.vitalcare.domain.model.VitalRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Room-backed vitals store — the app's only data source (D-005). Reads are
 * reactive Flows; commands are suspend functions returning [AppResult] (04 §8).
 */
interface VitalsRepository {
    fun observeAll(): Flow<List<VitalRecord>>
    fun observeByDate(date: LocalDate): Flow<List<VitalRecord>>
    fun observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<VitalRecord>>

    suspend fun getById(id: String): AppResult<VitalRecord>
    suspend fun save(record: VitalRecord): AppResult<Unit>

    /** Permanent hard delete (D-025). */
    suspend fun delete(id: String): AppResult<Unit>

    // Snapshot reads for CSV export (06 §3).
    suspend fun getAll(): AppResult<List<VitalRecord>>
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): AppResult<List<VitalRecord>>

    /** Restore-merge writes (D-024) — upsert only, never deletes. */
    suspend fun upsertAll(records: List<VitalRecord>): AppResult<Unit>
}
