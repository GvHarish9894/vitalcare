package com.techgv.vitalcare.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

internal const val DATABASE_NAME = "vitalcare.db"

@Database(entities = [VitalRecordEntity::class], version = 1, exportSchema = true)
@ConstructedBy(VitalCareDatabaseConstructor::class)
abstract class VitalCareDatabase : RoomDatabase() {
    abstract fun vitalRecordDao(): VitalRecordDao
}

// The Room compiler generates the per-target `actual` implementations.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object VitalCareDatabaseConstructor : RoomDatabaseConstructor<VitalCareDatabase> {
    override fun initialize(): VitalCareDatabase
}
