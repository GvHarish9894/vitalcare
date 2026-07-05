package com.techgv.vitalcare.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalRecordDao {

    @Upsert
    suspend fun upsert(record: VitalRecordEntity)

    @Upsert
    suspend fun upsertAll(records: List<VitalRecordEntity>)

    @Query("SELECT * FROM vital_records WHERE id = :id")
    suspend fun getById(id: String): VitalRecordEntity?

    // Reactive UI queries (FR-D5): newest first.
    @Query("SELECT * FROM vital_records WHERE date = :date ORDER BY time DESC")
    fun observeByDate(date: String): Flow<List<VitalRecordEntity>>

    @Query("SELECT * FROM vital_records ORDER BY date DESC, time DESC")
    fun observeAll(): Flow<List<VitalRecordEntity>>

    @Query("SELECT * FROM vital_records WHERE date BETWEEN :from AND :to ORDER BY date DESC, time DESC")
    fun observeByDateRange(from: String, to: String): Flow<List<VitalRecordEntity>>

    // Snapshot reads for CSV export / backup (oldest first, 06 §3).
    @Query("SELECT * FROM vital_records ORDER BY date ASC, time ASC")
    suspend fun getAll(): List<VitalRecordEntity>

    @Query("SELECT * FROM vital_records WHERE date BETWEEN :from AND :to ORDER BY date ASC, time ASC")
    suspend fun getByDateRange(from: String, to: String): List<VitalRecordEntity>

    /** Newest reading by civil date+time — reminder skip-if-recorded check (D-032). */
    @Query("SELECT * FROM vital_records ORDER BY date DESC, time DESC LIMIT 1")
    suspend fun getNewest(): VitalRecordEntity?

    /** Immediate permanent delete (D-025) — no tombstones. */
    @Query("DELETE FROM vital_records WHERE id = :id")
    suspend fun hardDelete(id: String)
}
