package com.techgv.vitalcare.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 → v2 (D-033): add the `fluid_entries` table (+ its `date` index).
 * `vital_records` is untouched. The column definitions and the index name
 * (`index_fluid_entries_date`) must match what Room generates for
 * [FluidEntryEntity] so the schema-validation check passes (06 §2).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `fluid_entries` (" +
                "`id` TEXT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`time` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`amountMl` INTEGER NOT NULL, " +
                "`note` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_fluid_entries_date` ON `fluid_entries` (`date`)",
        )
    }
}
