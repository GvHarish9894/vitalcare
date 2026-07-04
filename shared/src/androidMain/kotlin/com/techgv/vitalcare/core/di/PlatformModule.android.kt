package com.techgv.vitalcare.core.di

import android.content.Context
import androidx.room.RoomDatabase
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.techgv.vitalcare.data.local.VitalCareDatabase
import com.techgv.vitalcare.data.local.databaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<RoomDatabase.Builder<VitalCareDatabase>> { databaseBuilder(androidContext()) }
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("vitalcare_settings", Context.MODE_PRIVATE),
        )
    }
}
