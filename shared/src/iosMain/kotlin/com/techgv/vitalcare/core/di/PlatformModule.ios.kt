package com.techgv.vitalcare.core.di

import androidx.room.RoomDatabase
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.techgv.vitalcare.data.local.VitalCareDatabase
import com.techgv.vitalcare.data.local.databaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule: Module = module {
    single<RoomDatabase.Builder<VitalCareDatabase>> { databaseBuilder() }
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
}
