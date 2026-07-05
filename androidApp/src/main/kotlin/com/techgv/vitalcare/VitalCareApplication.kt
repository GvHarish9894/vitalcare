package com.techgv.vitalcare

import android.app.Application
import com.techgv.vitalcare.core.di.initKoin
import com.techgv.vitalcare.domain.backup.DriveConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class VitalCareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@VitalCareApplication)
            modules(module { single { DriveConfig(enabled = BuildConfig.DRIVE_ENABLED) } })
        }
    }
}
