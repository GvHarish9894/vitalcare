package com.techgv.vitalcare.core.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.techgv.vitalcare.core.telemetry.NoOpTelemetry
import com.techgv.vitalcare.core.telemetry.Telemetry
import com.techgv.vitalcare.core.util.CsvEncoder
import com.techgv.vitalcare.core.util.DefaultDispatcherProvider
import com.techgv.vitalcare.core.util.DispatcherProvider
import com.techgv.vitalcare.data.local.VitalCareDatabase
import com.techgv.vitalcare.data.settings.AppSettings
import kotlin.time.Clock
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform module contract (04 §5): each platform binds its Room database
 * builder and a [com.russhwolf.settings.Settings] instance (plus, later,
 * the file exporter).
 */
expect val platformModule: Module

val coreModule: Module = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<Clock> { Clock.System }
    single { CsvEncoder() }
    single<Telemetry> { NoOpTelemetry() }
    single { AppSettings(get()) }
}

val databaseModule: Module = module {
    single<VitalCareDatabase> {
        get<RoomDatabase.Builder<VitalCareDatabase>>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(get<DispatcherProvider>().io)
            .build()
    }
    single { get<VitalCareDatabase>().vitalRecordDao() }
}

// Filled in as the corresponding layers are built (repositories C6+, use
// cases C6+, view models C7+).
val repositoryModule: Module = module { }

val useCaseModule: Module = module { }

val viewModelModule: Module = module { }
