package com.techgv.vitalcare.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun databaseBuilder(context: Context): RoomDatabase.Builder<VitalCareDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<VitalCareDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
    )
}
