package com.techgv.vitalcare

import android.app.Application
import com.techgv.vitalcare.core.di.initKoin
import org.koin.android.ext.koin.androidContext

class VitalCareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@VitalCareApplication)
        }
    }
}
