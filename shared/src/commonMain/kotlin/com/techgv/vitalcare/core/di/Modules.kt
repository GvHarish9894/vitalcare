package com.techgv.vitalcare.core.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.techgv.vitalcare.core.telemetry.NoOpTelemetry
import com.techgv.vitalcare.core.telemetry.Telemetry
import com.techgv.vitalcare.core.util.CsvEncoder
import com.techgv.vitalcare.core.util.DefaultDispatcherProvider
import com.techgv.vitalcare.core.util.DispatcherProvider
import com.techgv.vitalcare.data.backup.BackupSerializer
import com.techgv.vitalcare.data.backup.DriveClient
import com.techgv.vitalcare.domain.backup.BackupRemote
import com.techgv.vitalcare.data.local.VitalCareDatabase
import com.techgv.vitalcare.data.repository.SettingsRepositoryImpl
import com.techgv.vitalcare.data.repository.VitalsRepositoryImpl
import com.techgv.vitalcare.data.settings.AppSettings
import com.techgv.vitalcare.domain.repository.SettingsRepository
import com.techgv.vitalcare.domain.repository.VitalsRepository
import com.techgv.vitalcare.domain.usecase.BackupNow
import com.techgv.vitalcare.domain.usecase.ConnectDrive
import com.techgv.vitalcare.domain.usecase.DeleteVitalRecord
import com.techgv.vitalcare.domain.usecase.DisconnectDrive
import com.techgv.vitalcare.domain.usecase.ExportCsv
import com.techgv.vitalcare.domain.usecase.GetAnalytics
import com.techgv.vitalcare.domain.usecase.GetHistory
import com.techgv.vitalcare.domain.usecase.GetTodaySummary
import com.techgv.vitalcare.domain.usecase.GetVitalRecord
import com.techgv.vitalcare.domain.usecase.MergeBackupRecords
import com.techgv.vitalcare.domain.usecase.ObserveBackupStatus
import com.techgv.vitalcare.domain.usecase.ObserveVitalRecord
import com.techgv.vitalcare.domain.usecase.RestoreFromDrive
import com.techgv.vitalcare.core.navigation.PendingNavigation
import com.techgv.vitalcare.domain.reminders.ReminderPermissionMonitor
import com.techgv.vitalcare.domain.usecase.SaveVitalRecord
import com.techgv.vitalcare.domain.usecase.SetAutoBackup
import com.techgv.vitalcare.domain.usecase.SetReminderPreferences
import com.techgv.vitalcare.domain.usecase.SyncReminders
import io.ktor.client.HttpClient
import com.techgv.vitalcare.domain.validation.VitalsValidator
import com.techgv.vitalcare.feature.analytics.AnalyticsViewModel
import com.techgv.vitalcare.feature.dashboard.DashboardViewModel
import com.techgv.vitalcare.feature.history.HistoryViewModel
import com.techgv.vitalcare.feature.history.RecordDetailsViewModel
import com.techgv.vitalcare.feature.settings.SettingsViewModel
import com.techgv.vitalcare.feature.vitals.RecordVitalsViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
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
    single<TimeZone> { TimeZone.currentSystemDefault() }
    single { CsvEncoder() }
    single<Telemetry> { NoOpTelemetry() }
    single { AppSettings(get()) }
    single { PendingNavigation() }
    single { ReminderPermissionMonitor(get()) }
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

val repositoryModule: Module = module {
    single<VitalsRepository> { VitalsRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get()) }
}

val useCaseModule: Module = module {
    factory { VitalsValidator(get(), get()) }
    factory { SaveVitalRecord(get(), get(), get(), get()) }
    factory { GetVitalRecord(get()) }
    factory { DeleteVitalRecord(get(), get(), get()) }
    factory { GetTodaySummary(get(), get(), get()) }
    factory { GetHistory(get(), get(), get()) }
    factory { ObserveVitalRecord(get()) }
    factory { GetAnalytics(get(), get(), get()) }
    factory { ExportCsv(get(), get(), get(), get(), get()) }
}

val backupModule: Module = module {
    single { HttpClient() }
    single { BackupSerializer() }
    single { MergeBackupRecords() }
    single<BackupRemote> { DriveClient(get()) }
    factory { ConnectDrive(get(), get()) }
    factory { DisconnectDrive(get(), get(), get()) }
    factory { BackupNow(get(), get(), get(), get(), get(), get(), get()) }
    factory { RestoreFromDrive(get(), get(), get(), get(), get()) }
    factory { SetAutoBackup(get(), get()) }
    factory { ObserveBackupStatus(get(), get()) }
}

val reminderModule: Module = module {
    factory { SyncReminders(get(), get(), get()) }
    factory { SetReminderPreferences(get(), get(), get()) }
}

val viewModelModule: Module = module {
    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { AnalyticsViewModel(get(), get(), get()) }
    viewModel {
        SettingsViewModel(
            settingsRepository = get(),
            exportCsv = get(),
            appInfo = get(),
            driveAuthorizer = get(),
            observeBackupStatus = get(),
            connectDrive = get(),
            disconnectDrive = get(),
            backupNow = get(),
            restoreFromDrive = get(),
            setAutoBackup = get(),
            setReminderPreferences = get(),
            reminderPermissionMonitor = get(),
            reminderPermission = get(),
            timeZone = get(),
        )
    }
    viewModel { params ->
        RecordDetailsViewModel(
            recordId = params.get(),
            observeVitalRecord = get(),
            deleteVitalRecord = get(),
            clock = get(),
            timeZone = get(),
        )
    }
    viewModel { params ->
        RecordVitalsViewModel(
            recordId = params.getOrNull(),
            saveVitalRecord = get(),
            getVitalRecord = get(),
            reminderScheduler = get(),
            clock = get(),
            timeZone = get(),
        )
    }
}
