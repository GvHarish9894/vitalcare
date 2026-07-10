package com.techgv.vitalcare.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * `fluid_entries` table (06 §2, D-033). Mirrors [VitalRecordEntity]'s
 * conventions: `date`/`time` are ISO-8601 strings (`yyyy-MM-dd` / `HH:mm`) so
 * text ordering matches chronological ordering; `type` is the [FluidType] enum
 * name; `amountMl` is canonical millilitres; timestamps are epoch millis UTC.
 */
@Entity(tableName = "fluid_entries", indices = [Index("date")])
data class FluidEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val time: String,
    val type: String,
    val amountMl: Int,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
