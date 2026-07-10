package com.techgv.vitalcare.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FluidEntryDao {

    @Upsert
    suspend fun upsert(entry: FluidEntryEntity)

    @Upsert
    suspend fun upsertAll(entries: List<FluidEntryEntity>)

    @Query("SELECT * FROM fluid_entries WHERE id = :id")
    suspend fun getById(id: String): FluidEntryEntity?

    // Reactive UI queries (FR-FL2): newest first.
    @Query("SELECT * FROM fluid_entries WHERE date = :date ORDER BY time DESC")
    fun observeByDate(date: String): Flow<List<FluidEntryEntity>>

    @Query("SELECT * FROM fluid_entries ORDER BY date DESC, time DESC")
    fun observeAll(): Flow<List<FluidEntryEntity>>

    @Query("SELECT * FROM fluid_entries WHERE date BETWEEN :from AND :to ORDER BY date DESC, time DESC")
    fun observeByDateRange(from: String, to: String): Flow<List<FluidEntryEntity>>

    // Snapshot reads for CSV export / backup (oldest first, 06 §3).
    @Query("SELECT * FROM fluid_entries ORDER BY date ASC, time ASC")
    suspend fun getAll(): List<FluidEntryEntity>

    @Query("SELECT * FROM fluid_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC")
    suspend fun getByDateRange(from: String, to: String): List<FluidEntryEntity>

    /** Immediate permanent delete (D-025) — no tombstones. */
    @Query("DELETE FROM fluid_entries WHERE id = :id")
    suspend fun hardDelete(id: String)
}
