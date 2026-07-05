package com.techgv.vitalcare.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * `vital_records` table (06 §2). `date`/`time` are ISO-8601 strings
 * (`yyyy-MM-dd` / `HH:mm`) so text ordering matches chronological ordering;
 * timestamps are epoch millis UTC (D-016). Null vitals mean "not measured".
 */
@Entity(tableName = "vital_records", indices = [Index("date")])
data class VitalRecordEntity(
    @PrimaryKey val id: String,
    val date: String,
    val time: String,
    val spo2: Int?,
    val heartRate: Int?,
    val systolic: Int?,
    val diastolic: Int?,
    val remarks: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
